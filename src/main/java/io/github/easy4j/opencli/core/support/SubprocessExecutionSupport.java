package io.github.easy4j.opencli.core.support;

import io.github.easy4j.opencli.core.OpenCliCancellationToken;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.CleanupState;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import java.time.Duration;

/**
 * Bounded, owned native-process execution. ProcessBuilder receives an exact argv
 * vector, never a shell string. Commons Exec handler/watchdog views remain for
 * compatibility with the earlier low-level RunSession API.
 */
public final class SubprocessExecutionSupport {
    @FunctionalInterface
    interface ProcessStarter {
        Process start(ProcessBuilder builder) throws IOException;
    }

    @FunctionalInterface
    interface WatchdogFactory {
        ExecuteWatchdog create(long timeoutMillis);
    }

    public static final long WAIT_GRACE_MILLIS = 5_000L;
    public static final int DEFAULT_STDOUT_LIMIT = 8 * 1024 * 1024;
    public static final int DEFAULT_STDERR_LIMIT = 2 * 1024 * 1024;
    private static final int READ_BUFFER_BYTES = 8192;
    private static final long POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(10L);
    private static final int DEFAULT_MAX_CONCURRENT = Math.max(2, java.lang.Runtime.getRuntime().availableProcessors());
    private static final Object LEGACY_LOCK = new Object();
    private static Runtime legacyRuntime = new Runtime(0);
    private static int legacySubmissions;

    private SubprocessExecutionSupport() { }

    /**
     * Configure only the deprecated static bridge, never an existing SDK client.
     * Reconfiguration with active or queued submissions is rejected rather than
     * creating a second live permit pool.
     *
     * @param maxConcurrent positive capacity or zero for the default
     * @deprecated pass an explicit OpenCliProcessRuntime to executors instead
     */
    @Deprecated
    public static void configureMaxConcurrentExecutions(int maxConcurrent) {
        synchronized (LEGACY_LOCK) {
            if (legacySubmissions != 0) {
                throw new IllegalStateException("Cannot reconfigure the legacy runtime while submissions exist");
            }
            legacyRuntime = new Runtime(maxConcurrent);
        }
    }

    public static int defaultMaxConcurrentExecutions() { return DEFAULT_MAX_CONCURRENT; }

    /** Legacy entry point using one stable, explicitly configured runtime. */
    public static RunSession execute(ExecutionRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        Runtime selected;
        synchronized (LEGACY_LOCK) {
            selected = legacyRuntime;
            legacySubmissions++;
        }
        try {
            return selected.execute(request);
        } finally {
            synchronized (LEGACY_LOCK) { legacySubmissions--; }
        }
    }

    /** Internal capacity/cleanup owner exposed through OpenCliProcessRuntime. */
    public static final class Runtime {
        private final int maxConcurrentExecutions;
        private final Semaphore permits;
        private final ProcessStarter processStarter;
        private final WatchdogFactory watchdogFactory;
        private volatile boolean quarantined;

        public Runtime(int maxConcurrent) {
            this(maxConcurrent, ProcessBuilder::start, SubprocessExecutionSupport::newWatchdog);
        }

        Runtime(int maxConcurrent, ProcessStarter processStarter, WatchdogFactory watchdogFactory) {
            if (maxConcurrent < 0) {
                throw new IllegalArgumentException("maxConcurrentExecutions must not be negative");
            }
            maxConcurrentExecutions = maxConcurrent == 0 ? DEFAULT_MAX_CONCURRENT : maxConcurrent;
            permits = new Semaphore(maxConcurrentExecutions, true);
            this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
            this.watchdogFactory = Objects.requireNonNull(watchdogFactory, "watchdogFactory");
        }

        public int getMaxConcurrentExecutions() { return maxConcurrentExecutions; }
        public boolean isQuarantined() { return quarantined; }

        public RunSession execute(ExecutionRequest request) throws IOException, InterruptedException {
            Objects.requireNonNull(request, "request");
            long budget = nanos(request.timeoutMillis, "timeoutMillis");
            BoundedCapture out = new BoundedCapture(request.stdoutLimitBytes);
            BoundedCapture err = new BoundedCapture(request.stderrLimitBytes);
            CleanupProcess process = null;
            Reader stdoutReader = null;
            Reader stderrReader = null;
            ExecuteWatchdog watchdog = null;
            TerminationReason reason = null;
            IOException ioFailure = null;
            boolean acquired = false;
            boolean interrupted = false;
            boolean cleanupOperationFailed = false;
            long queueWaitMillis = 0L;
            Integer exit = null;
            boolean streamsDrained = true;
            boolean cleanupFinished = true;
            try {
                while (!acquired && reason == null) {
                    if (Thread.interrupted()) {
                        interrupted = true;
                        reason = TerminationReason.CANCELLED;
                    } else if (request.cancellationToken.isCancelled()) {
                        reason = TerminationReason.CANCELLED;
                    } else if (quarantined) {
                        reason = TerminationReason.RUNTIME_UNAVAILABLE;
                    } else {
                        long remaining = remaining(request.submittedAtNanos, budget);
                        if (remaining <= 0) {
                            reason = TerminationReason.QUEUE_TIMEOUT;
                        } else {
                            acquired = permits.tryAcquire(Math.min(POLL_NANOS, remaining), TimeUnit.NANOSECONDS);
                        }
                    }
                }
                queueWaitMillis = elapsedMillis(request.submittedAtNanos);
                if (acquired && reason == null) {
                    if (Thread.interrupted()) {
                        interrupted = true;
                        reason = TerminationReason.CANCELLED;
                    } else if (request.cancellationToken.isCancelled()) {
                        reason = TerminationReason.CANCELLED;
                    } else if (quarantined) {
                        reason = TerminationReason.RUNTIME_UNAVAILABLE;
                    } else if (remaining(request.submittedAtNanos, budget) <= 0) {
                        reason = TerminationReason.QUEUE_TIMEOUT;
                    }
                }
                if (acquired && reason == null) {
                    ProcessBuilder builder = new ProcessBuilder(processArguments(request.nativeArgv));
                    if (request.workingDirectory != null) { builder.directory(request.workingDirectory); }
                    if (request.environment != null) {
                        builder.environment().clear();
                        builder.environment().putAll(request.environment);
                    }
                    process = new CleanupProcess(processStarter.start(builder));
                    process.getOutputStream().close();
                    stdoutReader = new Reader(process.getInputStream(), out, "opencli-stdout");
                    stderrReader = new Reader(process.getErrorStream(), err, "opencli-stderr");
                    stdoutReader.start();
                    stderrReader.start();
                    long remaining = remaining(request.submittedAtNanos, budget);
                    try {
                        watchdog = watchdogFactory.create(
                            Math.max(1L, TimeUnit.NANOSECONDS.toMillis(Math.max(0L, remaining))));
                        watchdog.start(process);
                    } catch (RuntimeException ex) {
                        ioFailure = new IOException("Failed to initialize execution watchdog", ex);
                        reason = TerminationReason.IO_FAILURE;
                    }
                    while (reason == null) {
                        IOException readFailure = readerFailure(stdoutReader, stderrReader);
                        if (out.isTruncated() || err.isTruncated()) {
                            reason = TerminationReason.OUTPUT_LIMIT;
                        } else if (Thread.interrupted()) {
                            interrupted = true;
                            reason = TerminationReason.CANCELLED;
                        } else if (request.cancellationToken.isCancelled()) {
                            reason = TerminationReason.CANCELLED;
                        } else if (Objects.nonNull(readFailure)) {
                            ioFailure = readFailure;
                            reason = TerminationReason.IO_FAILURE;
                        } else if (!process.isAlive() && !stdoutReader.isAlive() && !stderrReader.isAlive()) {
                            // A reader can publish its failure between the earlier check and thread exit.
                            ioFailure = readerFailure(stdoutReader, stderrReader);
                            if (Objects.nonNull(ioFailure)) {
                                reason = TerminationReason.IO_FAILURE;
                            } else {
                                reason = watchdog.killedProcess() ? TerminationReason.EXECUTION_TIMEOUT : TerminationReason.PROCESS_EXIT;
                            }
                        } else if (watchdog.killedProcess() || remaining(request.submittedAtNanos, budget) <= 0) {
                            reason = TerminationReason.EXECUTION_TIMEOUT;
                        } else {
                            TimeUnit.NANOSECONDS.sleep(Math.min(POLL_NANOS, Math.max(1L,
                                remaining(request.submittedAtNanos, budget))));
                        }
                    }
                }
            } catch (InterruptedException ex) {
                interrupted = true;
                reason = TerminationReason.CANCELLED;
            } catch (IOException ex) {
                ioFailure = ex;
                reason = process == null ? TerminationReason.SPAWN_FAILED : TerminationReason.IO_FAILURE;
            } catch (RuntimeException ex) {
                ioFailure = new IOException("Native process setup or execution failure", ex);
                reason = process == null ? TerminationReason.SPAWN_FAILED : TerminationReason.IO_FAILURE;
            } finally {
                try {
                    try {
                        if (Objects.nonNull(watchdog)) {
                            watchdog.stop();
                        }
                    } catch (RuntimeException ex) {
                        cleanupOperationFailed = true;
                    }
                    long cleanupStart = System.nanoTime();
                    long cleanupBudget = nanos(request.cleanupGraceMillis, "cleanupGraceMillis");
                    if (Objects.nonNull(process)) {
                        if (process.isAlive()) {
                            process.destroy();
                        }
                        long forceAfter = Math.min(TimeUnit.MILLISECONDS.toNanos(100L), cleanupBudget / 2L);
                        while (remaining(cleanupStart, cleanupBudget) > 0
                            && (process.isAlive() || alive(stdoutReader) || alive(stderrReader)
                                || process.hasPendingStop())) {
                            if (Thread.interrupted()) { interrupted = true; }
                            if (process.isAlive() && (process.graceful.failed
                                || System.nanoTime() - cleanupStart >= forceAfter)) {
                                process.destroyForcibly();
                            }
                            try {
                                TimeUnit.NANOSECONDS.sleep(Math.min(POLL_NANOS,
                                    Math.max(1L, remaining(cleanupStart, cleanupBudget))));
                            } catch (InterruptedException ex) {
                                interrupted = true;
                            }
                        }
                        if (process.isAlive()) {
                            process.destroyForcibly();
                        }
                        cleanupFinished = !process.hasPendingStop();
                        cleanupOperationFailed |= !cleanupFinished || process.graceful.failed || process.forced.failed;
                        if (!process.isAlive()) {
                            exit = process.exitValue();
                        }
                        streamsDrained = !alive(stdoutReader) && !alive(stderrReader);
                        if (Objects.isNull(exit) || !streamsDrained || !cleanupFinished) {
                            // Quarantine limits the number of unconfirmed children, readers and stop workers.
                            quarantined = true;
                            if (reason == TerminationReason.PROCESS_EXIT) { reason = TerminationReason.CLEANUP_UNCONFIRMED; }
                        }
                    }
                } finally {
                    out.freeze();
                    err.freeze();
                    if (acquired) { permits.release(); }
                    if (interrupted) { Thread.currentThread().interrupt(); }
                }
            }
            if (reason == TerminationReason.PROCESS_EXIT && (out.isTruncated() || err.isTruncated())) {
                reason = TerminationReason.OUTPUT_LIMIT;
            }
            DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            if (ioFailure != null) {
                handler.onProcessFailed(new ExecuteException("Native process I/O failure", exit == null ? -1 : exit));
            } else if (exit != null && exit != 0) {
                handler.onProcessFailed(new ExecuteException("Native process returned nonzero status", exit));
            } else if (exit != null) {
                handler.onProcessComplete(exit);
            }
            OpenCliExecutionDetails details = OpenCliExecutionDetails.builder()
                .terminationReason(reason)
                .cleanupState(process == null ? CleanupState.NOT_STARTED
                    : Objects.isNull(exit) || !streamsDrained || !cleanupFinished
                        ? CleanupState.UNCONFIRMED : CleanupState.ROOT_EXIT_CONFIRMED)
                .processStarted(process != null).streamsDrained(streamsDrained)
                .cleanupOperationFailed(cleanupOperationFailed)
                .stdoutCapturedBytes(out.size()).stdoutObservedBytes(out.observed()).stdoutTruncated(out.isTruncated())
                .stderrCapturedBytes(err.size()).stderrObservedBytes(err.observed()).stderrTruncated(err.isTruncated())
                .elapsedMillis(elapsedMillis(request.submittedAtNanos)).queueWaitMillis(queueWaitMillis)
                .descendantsExitConfirmed(false).build();
            // Preserve the legacy RunSession watchdog view even when nothing was started.
            ExecuteWatchdog watchdogView = Objects.nonNull(watchdog) ? watchdog : newWatchdog(request.timeoutMillis);
            return new RunSession(out, err, handler, watchdogView, request.timeoutMillis,
                reason == TerminationReason.QUEUE_TIMEOUT || reason == TerminationReason.EXECUTION_TIMEOUT,
                details, exit, ioFailure);
        }
    }

    private static ExecuteWatchdog newWatchdog(long timeoutMillis) {
        return ExecuteWatchdog.builder().setTimeout(Duration.ofMillis(timeoutMillis)).get();
    }

    private static boolean alive(Thread thread) { return thread != null && thread.isAlive(); }
    /**
     * The watchdog holds its monitor when invoking destroy. Keep native stop calls
     * outside that monitor and the caller, as closing process pipes can block.
     */
    private static final class CleanupProcess extends Process {
        private final Process delegate;
        private final StopRequest graceful = new StopRequest();
        private final StopRequest forced = new StopRequest();

        CleanupProcess(Process delegate) {
            this.delegate = delegate;
        }

        @Override
        public OutputStream getOutputStream() { return delegate.getOutputStream(); }

        @Override
        public InputStream getInputStream() { return delegate.getInputStream(); }

        @Override
        public InputStream getErrorStream() { return delegate.getErrorStream(); }

        @Override
        public int waitFor() throws InterruptedException { return delegate.waitFor(); }

        @Override
        public int exitValue() { return delegate.exitValue(); }

        @Override
        public boolean isAlive() { return delegate.isAlive(); }

        @Override
        public void destroy() {
            graceful.start("opencli-stop", delegate::destroy);
        }

        @Override
        public Process destroyForcibly() {
            forced.start("opencli-force-stop", delegate::destroyForcibly);
            return this;
        }

        boolean hasPendingStop() {
            return graceful.isPending() || forced.isPending();
        }
    }

    private static final class StopRequest {
        private Thread worker;
        private volatile boolean failed;

        synchronized void start(String name, Runnable signal) {
            if (Objects.nonNull(worker) || failed) {
                return;
            }
            try {
                worker = new Thread(() -> {
                    try {
                        signal.run();
                    } catch (RuntimeException ex) {
                        failed = true;
                    }
                }, name);
                worker.setDaemon(true);
                worker.start();
            } catch (RuntimeException ex) {
                failed = true;
            }
        }

        synchronized boolean isPending() {
            return alive(worker);
        }
    }

    private static long remaining(long start, long budget) { return budget - (System.nanoTime() - start); }
    // The Windows JDK legacy serializer treats a surrounding quote pair as already encoded.
    @SuppressWarnings("removal")
    private static List<String> processArguments(List<String> argv) {
        String executable = argv.get(0).toLowerCase(Locale.ROOT);
        if (!System.getProperty("os.name").startsWith("Windows") || !executable.endsWith(".exe")
            || executable.endsWith("cmd.exe") || Objects.nonNull(System.getSecurityManager())
            || "false".equalsIgnoreCase(System.getProperty("jdk.lang.Process.allowAmbiguousCommands"))) {
            return argv;
        }
        List<String> arguments = new ArrayList<>(argv);
        for (int i = 1; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            if (argument.indexOf('"') >= 0) {
                arguments.set(i, quoteWindowsExecutableArgument(argument));
            }
        }
        return arguments;
    }

    /** Windows CRT quoting for arguments with literal quotes in a native executable. */
    private static String quoteWindowsExecutableArgument(String argument) {
        StringBuilder quoted = new StringBuilder(argument.length() + 2).append('"');
        int backslashes = 0;
        for (int i = 0; i < argument.length(); i++) {
            char character = argument.charAt(i);
            if (character == '\\') {
                backslashes++;
                continue;
            }
            int escaped = character == '"' ? backslashes * 2 + 1 : backslashes;
            for (int j = 0; j < escaped; j++) {
                quoted.append('\\');
            }
            quoted.append(character);
            backslashes = 0;
        }
        for (int i = 0; i < backslashes * 2; i++) {
            quoted.append('\\');
        }
        return quoted.append('"').toString();
    }

    private static long elapsedMillis(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start); }

    private static long nanos(long millis, String field) {
        if (millis <= 0 || millis > Long.MAX_VALUE / 1_000_000L) {
            throw new IllegalArgumentException(field + " must be positive and fit a monotonic nanosecond budget");
        }
        return millis * 1_000_000L;
    }

    static final class BoundedCapture extends ByteArrayOutputStream {
        private final int limit;
        private long observed;
        private boolean truncated;
        private boolean frozen;

        BoundedCapture(int limit) {
            super(0);
            this.limit = limit;
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            if (frozen) { return; }
            observed = observed > Long.MAX_VALUE - length ? Long.MAX_VALUE : observed + length;
            int retained = Math.min(length, limit - count);
            int required = count + retained;
            if (required > buf.length) {
                int growth = (int) Math.min(limit, Math.max(READ_BUFFER_BYTES, 2L * buf.length));
                buf = Arrays.copyOf(buf, Math.max(required, growth));
            }
            super.write(bytes, offset, retained);
            truncated |= retained < length;
        }

        @Override
        public synchronized void write(int value) { write(new byte[]{(byte) value}, 0, 1); }
        synchronized int bufferCapacity() { return buf.length; }
        synchronized boolean isTruncated() { return truncated; }
        synchronized long observed() { return observed; }
        synchronized void freeze() { frozen = true; }
    }

    private static IOException readerFailure(Reader stdout, Reader stderr) {
        IOException failure = stdout.failure;
        return Objects.nonNull(failure) ? failure : stderr.failure;
    }

    private static final class Reader extends Thread {
        private final InputStream input;
        private final BoundedCapture capture;
        private volatile IOException failure;

        Reader(InputStream input, BoundedCapture capture, String name) {
            super(name);
            this.input = input;
            this.capture = capture;
            setDaemon(true);
        }

        @Override
        public void run() {
            try (InputStream stream = input) {
                byte[] buffer = new byte[READ_BUFFER_BYTES];
                int size;
                while ((size = stream.read(buffer)) != -1) {
                    capture.write(buffer, 0, size);
                    if (capture.isTruncated()) { return; }
                }
            } catch (IOException ex) {
                failure = ex;
            }
        }
    }

    @Getter
    public static final class ExecutionRequest {
        private final CommandLine commandLine;
        private final List<String> nativeArgv;
        private final File workingDirectory;
        private final Map<String, String> environment;
        private final long timeoutMillis;
        /** Requested legacy ceiling, or zero for independently configured stream limits. */
        private final long maxOutputBytes;
        private final int stdoutLimitBytes;
        private final int stderrLimitBytes;
        private final long cleanupGraceMillis;
        private final long submittedAtNanos;
        private final OpenCliCancellationToken cancellationToken;

        public ExecutionRequest(CommandLine commandLine, File workingDirectory,
            Map<String, String> environment, long timeoutMillis) {
            this(commandLine, workingDirectory, environment, timeoutMillis,
                DEFAULT_STDOUT_LIMIT, DEFAULT_STDERR_LIMIT, WAIT_GRACE_MILLIS,
                System.nanoTime(), new OpenCliCancellationToken());
        }

        /** Legacy per-stream ceiling; nonpositive values retain finite default budgets. */
        public ExecutionRequest(CommandLine commandLine, File workingDirectory,
            Map<String, String> environment, long timeoutMillis, long maxOutputBytes) {
            this(commandLine, workingDirectory, environment, timeoutMillis,
                legacyLimit(maxOutputBytes, DEFAULT_STDOUT_LIMIT),
                legacyLimit(maxOutputBytes, DEFAULT_STDERR_LIMIT), WAIT_GRACE_MILLIS,
                System.nanoTime(), new OpenCliCancellationToken(), maxOutputBytes);
        }

        private static int legacyLimit(long maxOutputBytes, int defaultLimit) {
            return maxOutputBytes > 0 ? (int) Math.min(defaultLimit, maxOutputBytes) : defaultLimit;
        }

        public ExecutionRequest(CommandLine commandLine, File workingDirectory,
            Map<String, String> environment, long timeoutMillis, int stdoutLimitBytes,
            int stderrLimitBytes, long cleanupGraceMillis, long submittedAtNanos,
            OpenCliCancellationToken cancellationToken) {
            this(commandLine, workingDirectory, environment, timeoutMillis, stdoutLimitBytes,
                stderrLimitBytes, cleanupGraceMillis, submittedAtNanos, cancellationToken, 0L);
        }

        private ExecutionRequest(CommandLine commandLine, File workingDirectory,
            Map<String, String> environment, long timeoutMillis, int stdoutLimitBytes,
            int stderrLimitBytes, long cleanupGraceMillis, long submittedAtNanos,
            OpenCliCancellationToken cancellationToken, long maxOutputBytes) {
            this.commandLine = Objects.requireNonNull(commandLine, "commandLine");
            List<String> argv = new ArrayList<>(Arrays.asList(commandLine.toStrings()));
            for (int i = 0; i < argv.size(); i++) {
                if (argv.get(i) == null) { throw new IllegalArgumentException("nativeArgv[" + i + "] must not be null"); }
            }
            nativeArgv = Collections.unmodifiableList(argv);
            this.workingDirectory = workingDirectory;
            this.environment = environment == null ? null : Collections.unmodifiableMap(new HashMap<>(environment));
            nanos(timeoutMillis, "timeoutMillis");
            nanos(cleanupGraceMillis, "cleanupGraceMillis");
            if (stdoutLimitBytes <= 0 || stderrLimitBytes <= 0) {
                throw new IllegalArgumentException("stdout/stderr capture budgets must be positive");
            }
            this.timeoutMillis = timeoutMillis;
            this.maxOutputBytes = maxOutputBytes;
            this.stdoutLimitBytes = stdoutLimitBytes;
            this.stderrLimitBytes = stderrLimitBytes;
            this.cleanupGraceMillis = cleanupGraceMillis;
            this.submittedAtNanos = submittedAtNanos;
            this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
        }
    }

    /** Bounded stdout or stderr snapshot returned by the low-level execution API. */
    public static final class BoundedOutputStream extends OutputStream {
        private final BoundedCapture capture;

        private BoundedOutputStream(BoundedCapture capture) {
            this.capture = capture;
        }

        @Override
        public void write(int value) {
            capture.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            capture.write(bytes, offset, length);
        }

        public byte[] toByteArray() {
            return capture.toByteArray();
        }

        public boolean isOverflowed() {
            return capture.isTruncated();
        }

        int bufferCapacity() {
            return capture.bufferCapacity();
        }
    }

    @Getter
    public static final class RunSession {
        private final BoundedOutputStream stdout;
        private final BoundedOutputStream stderr;
        private final DefaultExecuteResultHandler handler;
        private final ExecuteWatchdog watchdog;
        private final long timeoutMillis;
        private final boolean waitTimedOut;
        private final OpenCliExecutionDetails executionDetails;
        private final Integer observedExitCode;
        /** Raw low-level I/O evidence, which can include local paths. Exclude it from default diagnostics. */
        private final IOException ioFailure;

        RunSession(BoundedCapture stdout, BoundedCapture stderr,
            DefaultExecuteResultHandler handler, ExecuteWatchdog watchdog, long timeoutMillis,
            boolean waitTimedOut, OpenCliExecutionDetails executionDetails,
            Integer observedExitCode, IOException ioFailure) {
            this.stdout = new BoundedOutputStream(stdout);
            this.stderr = new BoundedOutputStream(stderr);
            this.handler = handler;
            this.watchdog = watchdog;
            this.timeoutMillis = timeoutMillis;
            this.waitTimedOut = waitTimedOut;
            this.executionDetails = executionDetails;
            this.observedExitCode = observedExitCode;
            this.ioFailure = ioFailure;
        }

        public boolean timedOut() { return waitTimedOut; }

        boolean isStdoutOverflowed() { return executionDetails.isStdoutTruncated(); }

        boolean isStderrOverflowed() { return executionDetails.isStderrTruncated(); }
    }
}
