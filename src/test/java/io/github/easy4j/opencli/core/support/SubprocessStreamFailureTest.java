package io.github.easy4j.opencli.core.support;

import io.github.easy4j.opencli.core.OpenCliCancellationToken;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.CleanupState;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that reader-thread I/O faults survive into the terminal result. */
@Timeout(5)
class SubprocessStreamFailureTest {

    @Test
    void partialOutputAndReadFailureRemainVisibleAfterRootExit() throws Exception {
        IOException readFailure = new IOException("injected stdout read failure");
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> new ExitedProcess(readFailure), timeout -> new IdleWatchdog());
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            CommandLine.parse("fake"), null, null, 1_000L, 32, 32, 100L,
            System.nanoTime(), new OpenCliCancellationToken());

        SubprocessExecutionSupport.RunSession session = runtime.execute(request);

        assertEquals(TerminationReason.IO_FAILURE,
            session.getExecutionDetails().getTerminationReason());
        assertSame(readFailure, session.getIoFailure());
        assertEquals("abc", session.getStdout().toString(StandardCharsets.UTF_8.name()));
        assertTrue(session.getExecutionDetails().isStreamsDrained());
    }

    @Test
    void openReaderKeepsCleanupUnconfirmedAfterRootExit() throws Exception {
        CountDownLatch releaseReader = new CountDownLatch(1);
        CountDownLatch readerClosed = new CountDownLatch(1);
        InputStream output = new InputStream() {
            @Override
            public int read() throws IOException {
                try {
                    releaseReader.await();
                    return -1;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException(ex);
                }
            }

            @Override
            public void close() {
                readerClosed.countDown();
            }
        };
        OpenCliCancellationToken token = new OpenCliCancellationToken();
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> new ExitedProcess(output), timeout -> new IdleWatchdog() {
                @Override
                public synchronized void start(Process process) {
                    token.cancel();
                }
            });
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            CommandLine.parse("fake"), null, null, 1_000L, 32, 32, 50L, System.nanoTime(), token);

        try {
            SubprocessExecutionSupport.RunSession session = runtime.execute(request);

            assertEquals(TerminationReason.CANCELLED, session.getExecutionDetails().getTerminationReason());
            assertEquals(0, session.getObservedExitCode());
            assertEquals(CleanupState.UNCONFIRMED, session.getExecutionDetails().getCleanupState());
            assertFalse(session.getExecutionDetails().isStreamsDrained());
            assertTrue(runtime.isQuarantined());
        } finally {
            releaseReader.countDown();
            assertTrue(readerClosed.await(1, TimeUnit.SECONDS));
        }
    }

    private static class IdleWatchdog extends ExecuteWatchdog {
        IdleWatchdog() { super(1_000L); }

        @Override
        public synchronized void start(Process process) { }
    }

    private static final class ExitedProcess extends Process {
        private final InputStream output;

        ExitedProcess(InputStream output) {
            this.output = output;
        }

        ExitedProcess(IOException readFailure) {
            this(new InputStream() {
                private boolean emitted;

                @Override
                public int read() throws IOException {
                    throw new UnsupportedOperationException("Reader uses block reads");
                }

                @Override
                public int read(byte[] bytes, int offset, int length) throws IOException {
                    if (emitted) { throw readFailure; }
                    byte[] partial = "abc".getBytes(StandardCharsets.UTF_8);
                    System.arraycopy(partial, 0, bytes, offset, partial.length);
                    emitted = true;
                    return partial.length;
                }
            });
        }

        @Override
        public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }

        @Override
        public InputStream getInputStream() { return output; }

        @Override
        public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }

        @Override
        public int waitFor() { return 0; }

        @Override
        public int exitValue() { return 0; }

        @Override
        public void destroy() { }

        @Override
        public boolean isAlive() { return false; }
    }
}
