package io.github.easy4j.opencli.core.support;

import io.github.easy4j.opencli.core.OpenCliCancellationToken;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.CleanupState;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteWatchdog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Fault injection for cleanup operations after an invocation-owned child starts. */
@Timeout(5)
class SubprocessCleanupFaultTest {
    @Test
    void destroyFailureStillForcesOwnedChildAndReleasesPermit() throws Exception {
        assertCleanupSurvives(false, true);
    }

    @Test
    void watchdogStopFailureStillCleansOwnedChildAndReleasesPermit() throws Exception {
        assertCleanupSurvives(true, false);
    }

    @Test
    void watchdogStartFailureStillCleansOwnedChildAndReleasesPermit() throws Exception {
        int[] starts = new int[1];
        FakeProcess[] last = new FakeProcess[1];
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> {
                starts[0]++;
                last[0] = new FakeProcess(false);
                return last[0];
            }, timeout -> new FakeWatchdog(false, true));

        for (int attempt = 0; attempt < 2; attempt++) {
            SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
                CommandLine.parse("fake"), null, null, 1000L, 16, 16, 200L,
                System.nanoTime(), new OpenCliCancellationToken());

            SubprocessExecutionSupport.RunSession session = runtime.execute(request);

            assertEquals(TerminationReason.IO_FAILURE,
                session.getExecutionDetails().getTerminationReason());
            assertInstanceOf(IllegalStateException.class, session.getIoFailure().getCause());
            assertEquals(CleanupState.ROOT_EXIT_CONFIRMED,
                session.getExecutionDetails().getCleanupState());
            assertFalse(last[0].isAlive());
            assertFalse(runtime.isQuarantined());
        }
        assertEquals(2, starts[0]);
    }

    @Test
    void watchdogCreationFailureAfterSpawnStillCleansChildAndReleasesPermit() throws Exception {
        int[] starts = new int[1];
        int[] watchdogCreations = new int[1];
        FakeProcess[] last = new FakeProcess[1];
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> {
                starts[0]++;
                last[0] = new FakeProcess(false);
                return last[0];
            }, timeout -> {
                if (++watchdogCreations[0] % 2 == 0) {
                    throw new IllegalStateException("injected watchdog creation failure");
                }
                return new FakeWatchdog(false);
            });

        for (int attempt = 0; attempt < 2; attempt++) {
            SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
                CommandLine.parse("fake"), null, null, 1000L, 16, 16, 200L,
                System.nanoTime(), new OpenCliCancellationToken());

            SubprocessExecutionSupport.RunSession session = runtime.execute(request);

            assertEquals(TerminationReason.IO_FAILURE,
                session.getExecutionDetails().getTerminationReason());
            assertInstanceOf(IllegalStateException.class, session.getIoFailure().getCause());
            assertEquals(CleanupState.ROOT_EXIT_CONFIRMED,
                session.getExecutionDetails().getCleanupState());
            assertFalse(last[0].isAlive());
            assertFalse(runtime.isQuarantined());
        }
        assertEquals(2, starts[0]);
    }

    @Test
    void processStreamSetupFailureStillCleansChildAndReleasesPermit() throws Exception {
        int[] starts = new int[1];
        FakeProcess[] last = new FakeProcess[1];
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> {
                starts[0]++;
                last[0] = new FakeProcess(false, false, true);
                return last[0];
            }, timeout -> new FakeWatchdog(false));

        for (int attempt = 0; attempt < 2; attempt++) {
            SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
                CommandLine.parse("fake"), null, null, 1000L, 16, 16, 200L,
                System.nanoTime(), new OpenCliCancellationToken());

            SubprocessExecutionSupport.RunSession session = runtime.execute(request);

            assertEquals(TerminationReason.IO_FAILURE,
                session.getExecutionDetails().getTerminationReason());
            assertInstanceOf(IllegalStateException.class, session.getIoFailure().getCause());
            assertEquals(CleanupState.ROOT_EXIT_CONFIRMED,
                session.getExecutionDetails().getCleanupState());
            assertFalse(last[0].isAlive());
            assertFalse(runtime.isQuarantined());
        }
        assertEquals(2, starts[0]);
    }

    @Test
    void unconfirmedForcedStopQuarantinesRuntimeWithoutFabricatingExit() throws Exception {
        OpenCliCancellationToken token = new OpenCliCancellationToken();
        int[] starts = new int[1];
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> {
                starts[0]++;
                token.cancel();
                return new FakeProcess(true, true);
            }, timeout -> new FakeWatchdog(false));
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            CommandLine.parse("fake"), null, null, 1000L, 16, 16, 50L,
            System.nanoTime(), token);

        SubprocessExecutionSupport.RunSession session = runtime.execute(request);

        assertEquals(TerminationReason.CANCELLED,
            session.getExecutionDetails().getTerminationReason());
        assertEquals(CleanupState.UNCONFIRMED,
            session.getExecutionDetails().getCleanupState());
        assertTrue(session.getExecutionDetails().isCleanupOperationFailed());
        assertNull(session.getObservedExitCode());
        assertTrue(runtime.isQuarantined());
        SubprocessExecutionSupport.ExecutionRequest next = new SubprocessExecutionSupport.ExecutionRequest(
            CommandLine.parse("fake"), null, null, 1000L, 16, 16, 50L,
            System.nanoTime(), new OpenCliCancellationToken());
        assertEquals(TerminationReason.RUNTIME_UNAVAILABLE,
            runtime.execute(next).getExecutionDetails().getTerminationReason());
        assertEquals(1, starts[0]);
    }

    private static void assertCleanupSurvives(boolean failStop, boolean failDestroy) throws Exception {
        OpenCliCancellationToken token = new OpenCliCancellationToken();
        AtomicReference<OpenCliCancellationToken> currentToken = new AtomicReference<>(token);
        FakeProcess[] last = new FakeProcess[1];
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> {
                last[0] = new FakeProcess(failDestroy);
                currentToken.get().cancel();
                return last[0];
            }, timeout -> new FakeWatchdog(failStop));
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            CommandLine.parse("fake"), null, null, 1000L, 16, 16, 200L,
            System.nanoTime(), token);

        SubprocessExecutionSupport.RunSession session = runtime.execute(request);

        assertEquals(TerminationReason.CANCELLED,
            session.getExecutionDetails().getTerminationReason());
        assertEquals(CleanupState.ROOT_EXIT_CONFIRMED,
            session.getExecutionDetails().getCleanupState());
        assertTrue(session.getExecutionDetails().isCleanupOperationFailed());
        assertFalse(last[0].isAlive());
        assertFalse(runtime.isQuarantined());

        OpenCliCancellationToken nextToken = new OpenCliCancellationToken();
        currentToken.set(nextToken);
        SubprocessExecutionSupport.ExecutionRequest next = new SubprocessExecutionSupport.ExecutionRequest(
            CommandLine.parse("fake"), null, null, 1000L, 16, 16, 200L,
            System.nanoTime(), nextToken);
        assertNotEquals(TerminationReason.QUEUE_TIMEOUT,
            runtime.execute(next).getExecutionDetails().getTerminationReason());
    }

    private static final class FakeWatchdog extends ExecuteWatchdog {
        private final boolean failStop;
        private final boolean failStart;

        FakeWatchdog(boolean failStop) {
            this(failStop, false);
        }

        FakeWatchdog(boolean failStop, boolean failStart) {
            super(1000L);
            this.failStop = failStop;
            this.failStart = failStart;
        }

        @Override
        public synchronized void start(Process process) {
            if (failStart) {
                throw new IllegalStateException("injected watchdog start failure");
            }
        }

        @Override
        public synchronized void stop() {
            if (failStop) {
                throw new IllegalStateException("injected watchdog stop failure");
            }
        }

        @Override
        public synchronized boolean killedProcess() { return false; }
    }

    private static final class FakeProcess extends Process {
        private final boolean failDestroy;
        private boolean alive = true;

        private final boolean failForce;
        private final boolean failStreamSetup;

        FakeProcess(boolean failDestroy) { this(failDestroy, false, false); }

        FakeProcess(boolean failDestroy, boolean failForce) {
            this(failDestroy, failForce, false);
        }

        FakeProcess(boolean failDestroy, boolean failForce, boolean failStreamSetup) {
            this.failDestroy = failDestroy;
            this.failForce = failForce;
            this.failStreamSetup = failStreamSetup;
        }

        @Override
        public OutputStream getOutputStream() {
            if (failStreamSetup) {
                throw new IllegalStateException("injected process stream setup failure");
            }
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(new byte[0]); }

        @Override
        public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }

        @Override
        public int waitFor() { return 143; }

        @Override
        public int exitValue() {
            if (alive) { throw new IllegalThreadStateException(); }
            return 143;
        }

        @Override
        public void destroy() {
            if (failDestroy) { throw new IllegalStateException("injected process destroy failure"); }
            alive = false;
        }

        @Override
        public Process destroyForcibly() {
            if (failForce) { throw new IllegalStateException("injected forced stop failure"); }
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() { return alive; }
    }
}
