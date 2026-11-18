package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.exception.OpenCliTimeoutException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** C02 tests observe actual fixture processes, not private semaphore counters. */
@Timeout(20)
class OpenCliProcessContractTest {
    @TempDir Path dir;

    private static OpenCliProperties properties(int maxConcurrent) {
        OpenCliProperties p = new OpenCliProperties();
        String exe = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        p.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), exe).getAbsolutePath());
        p.setLeadingArguments(new ArrayList<>(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            LifecycleProbe.class.getName())));
        p.setCommandTimeoutMillis(10000L);
        p.setMaxConcurrentExecutions(maxConcurrent);
        return p;
    }

    private static boolean awaitFile(Path path, long millis) throws Exception {
        long start = System.nanoTime();
        while (System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(millis)) {
            if (Files.exists(path)) { return true; }
            Thread.sleep(10L);
        }
        return Files.exists(path);
    }

    private static void release(Path path) {
        try { Files.write(path, new byte[]{1}); }
        catch (Exception ex) { throw new AssertionError("fixture cleanup failed", ex); }
    }

    private static Object getter(Object object, String name) {
        assertNotNull(object, "partial evidence is required");
        return assertDoesNotThrow(() -> object.getClass().getMethod(name).invoke(object),
            "required execution evidence is missing: " + name);
    }

    private static Object details(OpenCliResult result) { return getter(result, "getExecutionDetails"); }

    @Test
    void anotherClientCannotReplaceAnActiveClientsLimiter() throws Exception {
        OpenCliExecutor a = new OpenCliExecutor(properties(1));
        Path first = dir.resolve("first");
        Path second = dir.resolve("second");
        Path gate = dir.resolve("release");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<OpenCliResult> one = workers.submit(() -> a.invoke("hold", first.toString(), gate.toString()));
            assertTrue(awaitFile(first, 3000), "first child did not start");
            new OpenCliExecutor(properties(4));
            Future<OpenCliResult> two = workers.submit(() -> a.invoke("write", second.toString()));
            assertFalse(awaitFile(second, 600), "constructing B bypassed A's active limiter");
            release(gate);
            assertTrue(one.get(5, TimeUnit.SECONDS).isSuccess());
            assertTrue(two.get(5, TimeUnit.SECONDS).isSuccess());
        } finally {
            release(gate);
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void queuedDeadlineExpiresWithoutSpawning() throws Exception {
        OpenCliProperties p = properties(1);
        OpenCliExecutor executor = new OpenCliExecutor(p);
        Path first = dir.resolve("first");
        Path second = dir.resolve("must-not-start");
        Path gate = dir.resolve("release");
        ExecutorService worker = Executors.newSingleThreadExecutor();
        ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor();
        try {
            Future<OpenCliResult> one = worker.submit(() -> executor.invoke("hold", first.toString(), gate.toString()));
            assertTrue(awaitFile(first, 3000));
            p.setCommandTimeoutMillis(50L);
            cleanup.schedule(() -> release(gate), 1500L, TimeUnit.MILLISECONDS);
            long started = System.nanoTime();
            OpenCliTimeoutException failure = assertThrows(OpenCliTimeoutException.class,
                () -> executor.invoke("write", second.toString()));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertTrue(elapsed < 750L, "queue wait ignored total deadline: " + elapsed);
            assertFalse(Files.exists(second), "queue-expired child was started");
            Object evidence = details(failure.getPartialResult());
            assertEquals("QUEUE_TIMEOUT", String.valueOf(getter(evidence, "getTerminationReason")));
            assertEquals(false, getter(evidence, "isProcessStarted"));
            assertNull(failure.getPartialResult().getExitCode());
            release(gate);
            assertTrue(one.get(5, TimeUnit.SECONDS).isSuccess());
        } finally {
            release(gate);
            worker.shutdownNow();
            cleanup.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(cleanup.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void stdoutDefaultBudgetFailsInsteadOfReturningUnboundedSuccess() {
        OpenCliException failure = assertThrows(OpenCliException.class,
            () -> new OpenCliExecutor(properties(1)).invoke("stdout", Integer.toString(9 * 1024 * 1024)));
        OpenCliResult partial = failure.getPartialResult();
        Object evidence = details(partial);
        assertEquals("OUTPUT_LIMIT", String.valueOf(getter(evidence, "getTerminationReason")));
        assertEquals(8L * 1024 * 1024, ((Number) getter(evidence, "getStdoutCapturedBytes")).longValue());
        assertTrue(((Number) getter(evidence, "getStdoutObservedBytes")).longValue() > 8L * 1024 * 1024);
        assertEquals(true, getter(evidence, "isStdoutTruncated"));
        assertFalse(partial.isSuccess());
    }

    @Test
    void stderrHasItsOwnSmallerBudget() {
        OpenCliException failure = assertThrows(OpenCliException.class,
            () -> new OpenCliExecutor(properties(1)).invoke("stderr", Integer.toString(3 * 1024 * 1024)));
        Object evidence = details(failure.getPartialResult());
        assertEquals("OUTPUT_LIMIT", String.valueOf(getter(evidence, "getTerminationReason")));
        assertEquals(2L * 1024 * 1024, ((Number) getter(evidence, "getStderrCapturedBytes")).longValue());
        assertEquals(true, getter(evidence, "isStderrTruncated"));
    }

    @Test
    void interruptionStopsTheOwnedHeartbeatAndRestoresFlag() throws Exception {
        OpenCliExecutor executor = new OpenCliExecutor(properties(1));
        Path heartbeat = dir.resolve("heartbeat");
        Path gate = dir.resolve("release");
        AtomicBoolean restored = new AtomicBoolean();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread caller = new Thread(() -> {
            try { executor.invoke("heartbeat", heartbeat.toString(), gate.toString()); }
            catch (Throwable failure) { error.set(failure); restored.set(Thread.currentThread().isInterrupted()); }
        }, "contract-interrupted-caller");
        try {
            caller.start();
            assertTrue(awaitFile(heartbeat, 3000));
            Thread.sleep(80L);
            caller.interrupt();
            caller.join(1500L);
            assertFalse(caller.isAlive(), "interrupted call did not finish cleanup");
            assertTrue(error.get() instanceof OpenCliException);
            assertTrue(restored.get(), "caller interrupt flag was lost");
            String observed = new String(Files.readAllBytes(heartbeat), StandardCharsets.UTF_8);
            Thread.sleep(250L);
            assertEquals(observed, new String(Files.readAllBytes(heartbeat), StandardCharsets.UTF_8),
                "owned child kept running after interrupted call returned");
            OpenCliException failure = (OpenCliException) error.get();
            assertEquals("CANCELLED", String.valueOf(getter(details(failure.getPartialResult()), "getTerminationReason")));
            assertEquals("ROOT_EXIT_CONFIRMED", String.valueOf(getter(details(failure.getPartialResult()), "getCleanupState")));
            Path next = dir.resolve("next");
            assertTrue(executor.invoke("write", next.toString()).isSuccess(), "permit leaked after cancellation");
        } finally {
            release(gate);
            caller.interrupt();
            caller.join(5000L);
        }
    }

    @Test
    void negativeConcurrencyIsNotSilentlyTreatedAsDefault() {
        assertThrows(IllegalArgumentException.class, () -> new OpenCliExecutor(properties(-1)));
    }

    @Test
    void explicitSharedRuntimeLimitsBothClients() throws Exception {
        Class<?> runtimeType = assertDoesNotThrow(() -> Class.forName("io.github.easy4j.opencli.core.OpenCliProcessRuntime"));
        Object runtime = runtimeType.getConstructor(int.class).newInstance(1);
        OpenCliExecutor a = OpenCliExecutor.class.getConstructor(OpenCliProperties.class, runtimeType)
            .newInstance(properties(4), runtime);
        OpenCliExecutor b = OpenCliExecutor.class.getConstructor(OpenCliProperties.class, runtimeType)
            .newInstance(properties(4), runtime);
        Path first = dir.resolve("shared-first");
        Path second = dir.resolve("shared-second");
        Path gate = dir.resolve("release");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<OpenCliResult> one = workers.submit(() -> a.invoke("hold", first.toString(), gate.toString()));
            assertTrue(awaitFile(first, 3000));
            Future<OpenCliResult> two = workers.submit(() -> b.invoke("write", second.toString()));
            assertFalse(awaitFile(second, 500), "shared runtime did not enforce its one permit");
            release(gate);
            assertTrue(one.get(5, TimeUnit.SECONDS).isSuccess());
            assertTrue(two.get(5, TimeUnit.SECONDS).isSuccess());
        } finally {
            release(gate);
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void preCancelledRequestNeverStartsAProcess() throws Exception {
        Class<?> tokenType = assertDoesNotThrow(() -> Class.forName("io.github.easy4j.opencli.core.OpenCliCancellationToken"));
        Object token = tokenType.getConstructor().newInstance();
        tokenType.getMethod("cancel").invoke(token);
        Path marker = dir.resolve("pre-cancelled");
        OpenCliExecutor executor = new OpenCliExecutor(properties(1));
        java.lang.reflect.InvocationTargetException failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> OpenCliExecutor.class.getMethod("invoke", List.class, tokenType)
                .invoke(executor, Arrays.asList("write", marker.toString()), token));
        assertTrue(failure.getCause() instanceof OpenCliException);
        OpenCliResult partial = ((OpenCliException) failure.getCause()).getPartialResult();
        assertEquals("CANCELLED", String.valueOf(getter(details(partial), "getTerminationReason")));
        assertEquals(false, getter(details(partial), "isProcessStarted"));
        assertNull(partial.getExitCode());
        assertFalse(Files.exists(marker));
    }
}
