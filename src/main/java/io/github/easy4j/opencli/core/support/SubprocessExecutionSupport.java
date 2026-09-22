package io.github.easy4j.opencli.core.support;

import lombok.Getter;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Subprocess execution support based on Apache Commons Exec: watchdog timeout,
 * bounded {@code waitFor}, and concurrency throttling.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public final class SubprocessExecutionSupport {

    /** Watchdog 触发后，handler 收尾等待的上限（毫秒）。 */
    public static final long WAIT_GRACE_MILLIS = 5_000L;

    private static final int DEFAULT_MAX_CONCURRENT = Math.max(2, Runtime.getRuntime().availableProcessors());

    private static final AtomicReference<Semaphore> CONCURRENCY_LIMIT =
            new AtomicReference<>(new Semaphore(DEFAULT_MAX_CONCURRENT));

    private SubprocessExecutionSupport() {
    }

    /**
     * 配置本机 CLI 子进程全局并发上限；{@code maxConcurrent <= 0} 时恢复为默认值。
     *
     * @param maxConcurrent 允许同时运行的子进程数
     */
    public static void configureMaxConcurrentExecutions(int maxConcurrent) {
        if (maxConcurrent <= 0) {
            CONCURRENCY_LIMIT.set(new Semaphore(DEFAULT_MAX_CONCURRENT));
            return;
        }
        CONCURRENCY_LIMIT.set(new Semaphore(maxConcurrent));
    }

    /**
     * @return 未显式配置时的默认并发上限
     */
    public static int defaultMaxConcurrentExecutions() {
        return DEFAULT_MAX_CONCURRENT;
    }

    /**
     * 在并发许可内启动子进程并阻塞至结束、超时或被强制销毁。
     */
    public static RunSession execute(ExecutionRequest request) throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        Semaphore limit = CONCURRENCY_LIMIT.get();
        limit.acquire();
        try {
            return executeWithinLimit(request);
        } finally {
            limit.release();
        }
    }

    private static RunSession executeWithinLimit(ExecutionRequest request) throws IOException, InterruptedException {
        long timeoutMs = Math.max(1L, request.getTimeoutMillis());
        BoundedOutputStream out = new BoundedOutputStream(request.getMaxOutputBytes());
        BoundedOutputStream err = new BoundedOutputStream(request.getMaxOutputBytes());

        DefaultExecutor.Builder builder = DefaultExecutor.builder();
        if (request.getWorkingDirectory() != null) {
            builder.setWorkingDirectory(request.getWorkingDirectory());
        }
        DefaultExecutor executor = builder.get();
        executor.setStreamHandler(new PumpStreamHandler(out, err));

        ExecuteWatchdog watchdog =
                ExecuteWatchdog.builder().setTimeout(Duration.ofMillis(timeoutMs)).get();
        executor.setWatchdog(watchdog);

        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        Map<String, String> environment = request.getEnvironment();
        if (environment != null) {
            executor.execute(request.getCommandLine(), environment, handler);
        } else {
            executor.execute(request.getCommandLine(), handler);
        }

        boolean finished = awaitResult(handler, timeoutMs + WAIT_GRACE_MILLIS);
        boolean waitTimedOut = !finished;
        if (waitTimedOut) {
            watchdog.destroyProcess();
            awaitResult(handler, WAIT_GRACE_MILLIS);
        }

        return new RunSession(out, err, handler, watchdog, timeoutMs, waitTimedOut);
    }

    private static boolean awaitResult(DefaultExecuteResultHandler handler, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + Math.max(1L, timeoutMillis);
        while (!handler.hasResult()) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(Math.min(50L, deadline - System.currentTimeMillis()));
        }
        return true;
    }

    @Getter
    public static final class ExecutionRequest {

        private final CommandLine commandLine;
        private final File workingDirectory;
        private final Map<String, String> environment;
        private final long timeoutMillis;
        private final long maxOutputBytes;

        public ExecutionRequest(
                CommandLine commandLine,
                File workingDirectory,
                Map<String, String> environment,
                long timeoutMillis) {
                this(commandLine, workingDirectory, environment, timeoutMillis, 0L);
            }

        public ExecutionRequest(
                CommandLine commandLine,
                File workingDirectory,
                Map<String, String> environment,
                long timeoutMillis,
                long maxOutputBytes) {
                this.commandLine = Objects.requireNonNull(commandLine, "commandLine");
                this.workingDirectory = workingDirectory;
                this.environment = environment;
                this.timeoutMillis = timeoutMillis;
                this.maxOutputBytes = maxOutputBytes;
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

        RunSession(
                BoundedOutputStream stdout,
                BoundedOutputStream stderr,
                DefaultExecuteResultHandler handler,
                ExecuteWatchdog watchdog,
                long timeoutMillis,
                boolean waitTimedOut) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.handler = handler;
            this.watchdog = watchdog;
            this.timeoutMillis = timeoutMillis;
            this.waitTimedOut = waitTimedOut;
        }

        public boolean timedOut() {
            return waitTimedOut || watchdog.killedProcess();
        }

        boolean isStdoutOverflowed() {
            return stdout.isOverflowed();
        }

        boolean isStderrOverflowed() {
            return stderr.isOverflowed();
        }
    }

    /**
     * 有界内存输出流：超过上限的字节直接丢弃（保留前 maxBytes 字节），
     * 并置溢出标志供调用方追加截断标记。close 为空操作（纯内存流）。
     */
    public static final class BoundedOutputStream extends OutputStream {

        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private final long maxBytes;
        private boolean overflowed;

        BoundedOutputStream(long maxBytes) {
            // maxBytes <= 0 视为不限制
            this.maxBytes = Math.max(0L, maxBytes);
        }

        @Override
        public synchronized void write(int b) {
            write(new byte[] {(byte) b}, 0, 1);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            if (maxBytes == 0L) {
                delegate.write(b, off, len);
                return;
            }
            long remaining = maxBytes - delegate.size();
            if (remaining <= 0L) {
                overflowed = true;
                return;
            }
            if (len > remaining) {
                delegate.write(b, off, (int) remaining);
                overflowed = true;
            } else {
                delegate.write(b, off, len);
            }
        }

        public synchronized byte[] toByteArray() {
            return delegate.toByteArray();
        }

        public synchronized boolean isOverflowed() {
            return overflowed;
        }
    }
}
