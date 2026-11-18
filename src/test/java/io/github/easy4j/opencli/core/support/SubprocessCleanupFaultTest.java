package io.github.easy4j.opencli.core.support;

import io.github.easy4j.opencli.core.OpenCliCancellationToken;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.CleanupState;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
        FakeProcess[] last = new FakeProcess[1];
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> {
                starts[0]++;
                last[0] = new FakeProcess(false);
                return last[0];
            }, timeout -> {
                assertNotNull(last[0], "watchdog must be created after spawn");
                throw new IllegalStateException("injected watchdog creation failure");
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

    @Test
    void blockedGracefulAndForcedStopDoNotHoldCallerPastCleanupGrace() throws Exception {
        assertBoundedStop(false, TerminationReason.CANCELLED);
    }

    @Test
    void watchdogTimeoutDoesNotHoldCallerWhileNativeStopIsBlocked() throws Exception {
        assertBoundedStop(true, TerminationReason.EXECUTION_TIMEOUT);
    }

    private static void assertBoundedStop(boolean watchdogExpires, TerminationReason expected) throws Exception {
        CountDownLatch gracefulEntered = new CountDownLatch(1);
        CountDownLatch forceEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FakeProcess child = new FakeProcess(false) {
            @Override
            public void destroy() {
                waitForRelease(gracefulEntered, release);
                super.destroy();
            }

            @Override
            public Process destroyForcibly() {
                waitForRelease(forceEntered, release);
                return super.destroyForcibly();
            }
        };
        OpenCliCancellationToken token = new OpenCliCancellationToken();
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> {
                if (!watchdogExpires) {
                    token.cancel();
                }
                return child;
            }, timeout -> watchdogExpires ? new ExecuteWatchdog(20L) : new FakeWatchdog(false));
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            CommandLine.parse("fake"), null, null, 1000L, 16, 16, 250L, System.nanoTime(), token);

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<SubprocessExecutionSupport.RunSession> pending = worker.submit(() -> runtime.execute(request));
            assertTrue(gracefulEntered.await(1, TimeUnit.SECONDS));
            SubprocessExecutionSupport.RunSession session = pending.get(1250, TimeUnit.MILLISECONDS);
            assertEquals(0, forceEntered.getCount());
            assertEquals(expected, session.getExecutionDetails().getTerminationReason());
            assertEquals(CleanupState.UNCONFIRMED, session.getExecutionDetails().getCleanupState());
            assertTrue(session.getExecutionDetails().isCleanupOperationFailed());
            assertNull(session.getObservedExitCode());
            assertTrue(runtime.isQuarantined());
        } finally {
            release.countDown();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    private static void waitForRelease(CountDownLatch entered, CountDownLatch release) {
        entered.countDown();
        try {
            release.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void cancelledSubmissionDoesNotInitializeWatchdogOrSpawn() throws Exception {
        OpenCliCancellationToken token = new OpenCliCancellationToken();
        token.cancel();
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1,
            builder -> fail("cancelled submission must not spawn"),
            timeout -> fail("cancelled submission must not initialize a watchdog"));
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            CommandLine.parse("fake"), null, null, 1000L, 16, 16, 200L, System.nanoTime(), token);

        SubprocessExecutionSupport.RunSession session = runtime.execute(request);

        assertEquals(TerminationReason.CANCELLED, session.getExecutionDetails().getTerminationReason());
        assertEquals(CleanupState.NOT_STARTED, session.getExecutionDetails().getCleanupState());
        assertNull(session.getObservedExitCode());
        assertNotNull(session.getWatchdog());
        assertEquals(0, session.getStdout().bufferCapacity());
        assertEquals(0, session.getStderr().bufferCapacity());
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

    private static class FakeProcess extends Process {
        private final boolean failDestroy;
        private volatile boolean alive = true;

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
