package io.github.easy4j.opencli.core.support;

import io.github.easy4j.opencli.core.OpenCliCancellationToken;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.CleanupState;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies terminal evidence when cancellation, watchdog expiry and natural exit coincide. */
@Timeout(10)
class SubprocessCompletionRaceTest {

    @Test
    void simultaneousSignalsKeepOneResultAndReuseThePermit() throws Exception {
        AtomicInteger starts = new AtomicInteger();
        AtomicReference<OpenCliCancellationToken> currentToken = new AtomicReference<>();
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> {
                starts.incrementAndGet();
                currentToken.get().cancel();
                return new ExitedProcess("partial");
            }, timeout -> new ExpiredWatchdog());

        for (int attempt = 0; attempt < 32; attempt++) {
            OpenCliCancellationToken token = new OpenCliCancellationToken();
            currentToken.set(token);
            SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
                CommandLine.parse("fake"), null, null, 1_000L, 32, 32, 100L,
                System.nanoTime(), token);
            SubprocessExecutionSupport.RunSession session = runtime.execute(request);

            assertEquals(TerminationReason.CANCELLED,
                session.getExecutionDetails().getTerminationReason());
            assertEquals(CleanupState.ROOT_EXIT_CONFIRMED,
                session.getExecutionDetails().getCleanupState());
            assertEquals(0, session.getObservedExitCode());
            assertEquals("partial", session.getStdout().toString(StandardCharsets.UTF_8.name()));
            assertEquals(7, session.getExecutionDetails().getStdoutCapturedBytes());
            assertTrue(session.getExecutionDetails().isStreamsDrained());
            assertFalse(runtime.isQuarantined());
        }
        assertEquals(32, starts.get());
    }

    private static final class ExpiredWatchdog extends ExecuteWatchdog {
        ExpiredWatchdog() { super(1_000L); }

        @Override
        public synchronized void start(Process process) { }

        @Override
        public synchronized boolean killedProcess() { return true; }
    }

    private static final class ExitedProcess extends Process {
        private final InputStream output;

        ExitedProcess(String text) {
            output = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
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
