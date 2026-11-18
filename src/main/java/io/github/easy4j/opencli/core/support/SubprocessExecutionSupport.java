package io.github.easy4j.opencli.core.support;

import io.github.easy4j.opencli.core.OpenCliCancellationToken;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.CleanupState;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
            Process process = null;
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
                    ProcessBuilder builder = new ProcessBuilder(request.nativeArgv);
                    if (request.workingDirectory != null) { builder.directory(request.workingDirectory); }
                    if (request.environment != null) {
                        builder.environment().clear();
                        builder.environment().putAll(request.environment);
                    }
                    process = processStarter.start(builder);
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
                        if (out.isTruncated() || err.isTruncated()) {
                            reason = TerminationReason.OUTPUT_LIMIT;
                        } else if (Thread.interrupted()) {
                            interrupted = true;
                            reason = TerminationReason.CANCELLED;
                        } else if (request.cancellationToken.isCancelled()) {
                            reason = TerminationReason.CANCELLED;
                        } else if (Objects.nonNull(stdoutReader.failure) || Objects.nonNull(stderrReader.failure)) {
                            ioFailure = Objects.nonNull(stdoutReader.failure)
                                ? stdoutReader.failure : stderrReader.failure;
                            reason = TerminationReason.IO_FAILURE;
                        } else if (!process.isAlive() && !stdoutReader.isAlive() && !stderrReader.isAlive()) {
                            reason = watchdog.killedProcess() ? TerminationReason.EXECUTION_TIMEOUT : TerminationReason.PROCESS_EXIT;
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
                        boolean forceSent = false;
                        if (process.isAlive()) {
                            try {
                                process.destroy();
                            } catch (RuntimeException ex) {
                                cleanupOperationFailed = true;
                                forceSent = true;
                                if (!tryForceStop(process)) { cleanupOperationFailed = true; }
                            }
                        }
                        while (remaining(cleanupStart, cleanupBudget) > 0
                            && (process.isAlive() || alive(stdoutReader) || alive(stderrReader))) {
                            if (Thread.interrupted()) { interrupted = true; }
                            if (process.isAlive() && !forceSent
                                && System.nanoTime() - cleanupStart >= Math.min(TimeUnit.MILLISECONDS.toNanos(100L), cleanupBudget / 2)) {
                                if (!tryForceStop(process)) { cleanupOperationFailed = true; }
                                forceSent = true;
                            }
                            try {
                                TimeUnit.NANOSECONDS.sleep(Math.min(POLL_NANOS,
                                    Math.max(1L, remaining(cleanupStart, cleanupBudget))));
                            } catch (InterruptedException ex) {
                                interrupted = true;
                            }
                        }
                        if (process.isAlive() && !tryForceStop(process)) { cleanupOperationFailed = true; }
                        if (!process.isAlive()) {
                            exit = process.exitValue();
                        }
                        streamsDrained = !alive(stdoutReader) && !alive(stderrReader);
                        if (Objects.isNull(exit) || !streamsDrained) {
                            // A fixed runtime cannot accumulate unlimited uncertain children/readers.
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
                handler.onProcessFailed(new ExecuteException("Native process I/O failure", exit == null ? -1 : exit, ioFailure));
            } else if (exit != null && exit != 0) {
                handler.onProcessFailed(new ExecuteException("Native process returned nonzero status", exit));
            } else if (exit != null) {
                handler.onProcessComplete(exit);
            }
            OpenCliExecutionDetails details = OpenCliExecutionDetails.builder()
                .terminationReason(reason)
                .cleanupState(process == null ? CleanupState.NOT_STARTED
                    : Objects.isNull(exit) || !streamsDrained ? CleanupState.UNCONFIRMED : CleanupState.ROOT_EXIT_CONFIRMED)
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
    private static boolean tryForceStop(Process process) {
        try {
            process.destroyForcibly();
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
    private static long remaining(long start, long budget) { return budget - (System.nanoTime() - start); }
    private static long elapsedMillis(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start); }

    private static long nanos(long millis, String field) {
        if (millis <= 0 || millis > Long.MAX_VALUE / 1_000_000L) {
            throw new IllegalArgumentException(field + " must be positive and fit a monotonic nanosecond budget");
        }
        return millis * 1_000_000L;
    }

    private static final class BoundedCapture extends ByteArrayOutputStream {
        private final int limit;
        private long observed;
        private boolean truncated;
        private boolean frozen;

        BoundedCapture(int limit) {
            super(Math.min(8192, limit));
            this.limit = limit;
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            if (frozen) { return; }
            observed = observed > Long.MAX_VALUE - length ? Long.MAX_VALUE : observed + length;
            int retained = Math.min(length, limit - count);
            super.write(bytes, offset, retained);
            truncated |= retained < length;
        }

        @Override
        public synchronized void write(int value) { write(new byte[]{(byte) value}, 0, 1); }
        synchronized boolean isTruncated() { return truncated; }
        synchronized long observed() { return observed; }
        synchronized void freeze() { frozen = true; }
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
                byte[] buffer = new byte[8192];
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
                System.nanoTime(), new OpenCliCancellationToken());
        }

        private static int legacyLimit(long maxOutputBytes, int defaultLimit) {
            return maxOutputBytes > 0 ? (int) Math.min(defaultLimit, maxOutputBytes) : defaultLimit;
        }

        public ExecutionRequest(CommandLine commandLine, File workingDirectory,
            Map<String, String> environment, long timeoutMillis, int stdoutLimitBytes,
            int stderrLimitBytes, long cleanupGraceMillis, long submittedAtNanos,
            OpenCliCancellationToken cancellationToken) {
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
            this.stdoutLimitBytes = stdoutLimitBytes;
            this.stderrLimitBytes = stderrLimitBytes;
            this.cleanupGraceMillis = cleanupGraceMillis;
            this.submittedAtNanos = submittedAtNanos;
            this.cancellationToken = Objects.requireNonNull(cancellationToken, "cancellationToken");
        }
    }

    @Getter
    public static final class RunSession {
        private final ByteArrayOutputStream stdout;
        private final ByteArrayOutputStream stderr;
        private final DefaultExecuteResultHandler handler;
        private final ExecuteWatchdog watchdog;
        private final long timeoutMillis;
        private final boolean waitTimedOut;
        private final OpenCliExecutionDetails executionDetails;
        private final Integer observedExitCode;
        private final IOException ioFailure;

        RunSession(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr,
            DefaultExecuteResultHandler handler, ExecuteWatchdog watchdog, long timeoutMillis,
            boolean waitTimedOut, OpenCliExecutionDetails executionDetails,
            Integer observedExitCode, IOException ioFailure) {
            this.stdout = stdout;
            this.stderr = stderr;
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
