package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliCancellationToken;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Additional finite-budget and immutable-submission regression vectors. */
@Timeout(20)
class OpenCliProcessBoundaryTest {
    @TempDir Path dir;

    private static OpenCliProperties properties() {
        OpenCliProperties p = new OpenCliProperties();
        String exe = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        p.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), exe).getAbsolutePath());
        p.setLeadingArguments(new ArrayList<>(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            LifecycleProbe.class.getName())));
        p.setCommandTimeoutMillis(10000L);
        p.setMaxConcurrentExecutions(1);
        return p;
    }

    private static void await(Path path) throws Exception {
        long started = System.nanoTime();
        while (!Files.exists(path) && System.nanoTime() - started < TimeUnit.SECONDS.toNanos(3L)) { Thread.sleep(10L); }
        assertTrue(Files.exists(path), "fixture did not start");
    }

    @Test
    void utf8TruncationReportsBytesNotReencodedCharacters() {
        OpenCliProperties p = properties();
        p.setMaxStdoutBytes(4);
        OpenCliException failure = assertThrows(OpenCliException.class, () -> new OpenCliExecutor(p).invoke("utf8"));
        OpenCliResult partial = failure.getPartialResult();
        assertNotNull(partial);
        assertEquals(TerminationReason.OUTPUT_LIMIT, partial.getExecutionDetails().getTerminationReason());
        assertEquals(4L, partial.getExecutionDetails().getStdoutCapturedBytes());
        assertEquals(6L, partial.getExecutionDetails().getStdoutObservedBytes());
        assertEquals("中\uFFFD", partial.getStdout());
        assertTrue(partial.getExecutionDetails().isStdoutTruncated());
    }

    @Test
    void invalidBudgetsFailBeforeChildCreation() {
        for (int choice = 0; choice < 5; choice++) {
            OpenCliProperties p = properties();
            if (choice == 0) { p.setMaxStdoutBytes(0); }
            if (choice == 1) { p.setMaxStderrBytes(-1); }
            if (choice == 2) { p.setCleanupGraceMillis(0); }
            if (choice == 3) { p.setCommandTimeoutMillis(Long.MAX_VALUE); }
            if (choice == 4) { p.setCleanupGraceMillis(Long.MAX_VALUE); }
            Path marker = dir.resolve("invalid-" + choice);
            assertThrows(IllegalArgumentException.class, () -> new OpenCliExecutor(p).invoke("write", marker.toString()));
            assertFalse(Files.exists(marker));
        }
    }

    @Test
    void explicitCancellationOfRunningChildRetainsBoundedEvidence() throws Exception {
        OpenCliExecutor executor = new OpenCliExecutor(properties());
        OpenCliCancellationToken token = new OpenCliCancellationToken();
        Path heartbeat = dir.resolve("heartbeat");
        Path release = dir.resolve("release");
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<OpenCliResult> future = worker.submit(() -> executor.invoke(
                Arrays.asList("heartbeat", heartbeat.toString(), release.toString()), token));
            await(heartbeat);
            token.cancel();
            ExecutionException failed = assertThrows(ExecutionException.class, () -> future.get(3, TimeUnit.SECONDS));
            assertTrue(failed.getCause() instanceof OpenCliException);
            OpenCliResult partial = ((OpenCliException) failed.getCause()).getPartialResult();
            assertEquals(TerminationReason.CANCELLED, partial.getExecutionDetails().getTerminationReason());
            assertTrue(partial.getExecutionDetails().isProcessStarted());
            assertFalse(partial.getExecutionDetails().isDescendantsExitConfirmed());
            String stopped = new String(Files.readAllBytes(heartbeat), StandardCharsets.UTF_8);
            Thread.sleep(100L);
            assertEquals(stopped, new String(Files.readAllBytes(heartbeat), StandardCharsets.UTF_8));
            assertTrue(executor.invoke("write", dir.resolve("next").toString()).isSuccess());
        } finally {
            Files.write(release, new byte[]{1});
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void queuedTokenCancellationDoesNotStartTheWaitingChild() throws Exception {
        OpenCliExecutor executor = new OpenCliExecutor(properties());
        Path first = dir.resolve("first");
        Path second = dir.resolve("second");
        Path gate = dir.resolve("release");
        OpenCliCancellationToken token = new OpenCliCancellationToken();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<OpenCliResult> one = workers.submit(() -> executor.invoke("hold", first.toString(), gate.toString()));
            await(first);
            Future<OpenCliResult> two = workers.submit(() -> executor.invoke(Arrays.asList("write", second.toString()), token));
            Thread.sleep(100L);
            token.cancel();
            ExecutionException failed = assertThrows(ExecutionException.class, () -> two.get(2, TimeUnit.SECONDS));
            OpenCliResult partial = ((OpenCliException) failed.getCause()).getPartialResult();
            assertEquals(TerminationReason.CANCELLED, partial.getExecutionDetails().getTerminationReason());
            assertFalse(partial.getExecutionDetails().isProcessStarted());
            assertNull(partial.getExitCode());
            assertFalse(Files.exists(second));
            Files.write(gate, new byte[]{1});
            assertTrue(one.get(3, TimeUnit.SECONDS).isSuccess());
        } finally {
            Files.write(gate, new byte[]{1});
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void timeoutAndCaptureConfigurationAreCopiedForReverseWorkers() {
        OpenCliProperties p = properties();
        p.setMaxStdoutBytes(123);
        p.setMaxStderrBytes(456);
        p.setCleanupGraceMillis(789);
        OpenCliProperties copy = p.copyForLocalCliExecution();
        assertEquals(123, copy.getMaxStdoutBytes());
        assertEquals(456, copy.getMaxStderrBytes());
        assertEquals(789L, copy.getCleanupGraceMillis());
    }

    @Test
    void repeatedExecutionTimeoutsDoNotBecomeNonzeroOrIoFailures() {
        OpenCliProperties p = properties();
        p.setCommandTimeoutMillis(100L);
        OpenCliExecutor executor = new OpenCliExecutor(p);
        for (int i = 0; i < 5; i++) {
            Path marker = dir.resolve("timeout-" + i);
            OpenCliTimeoutException failure = assertThrows(OpenCliTimeoutException.class,
                () -> executor.invoke("hold", marker.toString(), dir.resolve("never-release").toString()));
            assertEquals(TerminationReason.EXECUTION_TIMEOUT, failure.getPartialResult().getExecutionDetails().getTerminationReason());
            assertFalse(executor.getProcessRuntime().isQuarantined());
        }
    }
}
