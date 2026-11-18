package io.github.easy4j.opencli.core.support;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import io.github.easy4j.opencli.contract.LifecycleProbe;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@org.junit.jupiter.api.parallel.Isolated
class SubprocessExecutionSupportTest {

    @Test
    void shouldKeepNativePathOutOfDefaultHandlerException(@TempDir Path temporary) throws Exception {
        String canary = "private-native-path";
        CommandLine command = new CommandLine(temporary.resolve(canary + ".exe").toString());
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            command, null, null, 5_000L);

        SubprocessExecutionSupport.RunSession session = SubprocessExecutionSupport.execute(request);

        assertEquals(TerminationReason.SPAWN_FAILED, session.getExecutionDetails().getTerminationReason());
        assertNotNull(session.getIoFailure());
        assertTrue(session.getIoFailure().getMessage().contains(canary));
        assertNotNull(session.getHandler().getException());
        StringWriter diagnostics = new StringWriter();
        session.getHandler().getException().printStackTrace(new PrintWriter(diagnostics));
        assertFalse(diagnostics.toString().contains(canary));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fixture.exe", "C:\\tools\\fixturecmd.exe", "C:/tools/fixturecmd.exe", "node"})
    void rejectsQuotedWindowsArgumentsBeforeNativeSpawnInStrictMode(String executable) throws Exception {
        String previousOs = System.getProperty("os.name");
        String property = "jdk.lang.Process.allowAmbiguousCommands";
        String previousMode = System.getProperty(property);
        AtomicInteger launches = new AtomicInteger();
        SubprocessExecutionSupport.Runtime runtime = new SubprocessExecutionSupport.Runtime(1, builder -> {
            launches.incrementAndGet();
            throw new IOException("fixture must not launch");
        }, timeout -> new org.apache.commons.exec.ExecuteWatchdog(timeout));
        CommandLine command = new CommandLine(executable) {
            @Override
            public String[] toStrings() {
                return new String[]{executable, "\"private-quoted-value\""};
            }
        };
        SubprocessExecutionSupport.ExecutionRequest request =
            new SubprocessExecutionSupport.ExecutionRequest(command, null, null, 1_000L);
        try {
            System.setProperty("os.name", "Windows");
            System.setProperty(property, "false");
            UnsupportedOperationException failure = assertThrows(
                UnsupportedOperationException.class, () -> runtime.execute(request));
            assertTrue(failure.getMessage().contains("argv[1]"));
            assertFalse(failure.getMessage().contains("private-quoted-value"));
            assertEquals(0, launches.get());
        } finally {
            System.setProperty("os.name", previousOs);
            if (previousMode == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousMode);
            }
        }
    }

    @Test
    void shouldReturnDefaultMaxConcurrent() {
        int defaultVal = SubprocessExecutionSupport.defaultMaxConcurrentExecutions();
        assertTrue(defaultVal >= 2);
    }

    @Test
    void shouldConfigureMaxConcurrentExecutions() {
        SubprocessExecutionSupport.configureMaxConcurrentExecutions(8);
        // no assertion on internal state, just ensure no exception
        SubprocessExecutionSupport.configureMaxConcurrentExecutions(0); // reset to default
    }

    @Test
    void shouldHaveWaitGraceMillis() {
        assertEquals(5_000L, SubprocessExecutionSupport.WAIT_GRACE_MILLIS);
    }

    @Test
    void shouldRejectNullRequest() {
        assertThrows(NullPointerException.class, () -> SubprocessExecutionSupport.execute(null));
    }

    @Test
    void shouldExecuteSimpleCommand() throws Exception {
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            org.apache.commons.exec.CommandLine.parse("echo hello"),
            null, null, 10_000L);
        SubprocessExecutionSupport.RunSession session = SubprocessExecutionSupport.execute(request);
        assertNotNull(session.getStdout());
        assertFalse(session.timedOut());
    }

    @Test
    void shouldCapCapturedOutputAtMaxBytes() throws Exception {
        org.apache.commons.exec.CommandLine cmd =
            org.apache.commons.exec.CommandLine.parse("printf aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            cmd, null, null, 10_000L, 16L);
        SubprocessExecutionSupport.RunSession session = SubprocessExecutionSupport.execute(request);
        SubprocessExecutionSupport.BoundedOutputStream stdout = session.getStdout();
        SubprocessExecutionSupport.BoundedOutputStream stderr = session.getStderr();
        assertEquals(16L, request.getMaxOutputBytes());
        assertEquals(16, stdout.toByteArray().length);
        assertTrue(stdout.isOverflowed());
        assertFalse(stderr.isOverflowed());
        assertTrue(session.isStdoutOverflowed());
    }

    @Test
    void legacyOutputStreamsAllowBoundedWritesWithoutChangingExecutionDetails() throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        File java = new File(new File(System.getProperty("java.home"), "bin"), executable);
        CommandLine command = new CommandLine(java);
        command.addArguments(new String[] {"-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            LifecycleProbe.class.getName(), "stdout", "3"}, false);
        SubprocessExecutionSupport.RunSession session = SubprocessExecutionSupport.execute(
            new SubprocessExecutionSupport.ExecutionRequest(command, null, null, 10_000L, 5L));

        SubprocessExecutionSupport.BoundedOutputStream stdout = session.getStdout();
        SubprocessExecutionSupport.BoundedOutputStream stderr = session.getStderr();
        stdout.write('d');
        stdout.write(new byte[]{'x', 'e', 'f'}, 1, 2);
        stderr.write(new byte[]{'!'}, 0, 1);

        assertArrayEquals("xxxde".getBytes(StandardCharsets.UTF_8), stdout.toByteArray());
        assertTrue(stdout.isOverflowed());
        assertArrayEquals(new byte[]{'!'}, stderr.toByteArray());
        assertFalse(session.getExecutionDetails().isStdoutTruncated());
        assertFalse(session.getExecutionDetails().isStderrTruncated());
    }

    @Test
    void shouldAllocateOutputStorageOnlyOnFirstNonemptyWrite() {
        SubprocessExecutionSupport.BoundedCapture capture = new SubprocessExecutionSupport.BoundedCapture(12_000);
        byte[] empty = new byte[0];
        capture.write(empty, 0, empty.length);

        assertEquals(0, capture.bufferCapacity());
        capture.write(99);
        assertEquals(1, capture.size());
        assertEquals(99, capture.toByteArray()[0]);
        assertEquals(8_192, capture.bufferCapacity());
    }

    @Test
    void shouldBoundBackingBufferGrowthAtTheConfiguredLimit() {
        int limit = 8_193;
        SubprocessExecutionSupport.BoundedCapture capture = new SubprocessExecutionSupport.BoundedCapture(limit);
        byte[] chunk = new byte[8_192];
        capture.write(chunk, 0, chunk.length);
        capture.write(99);
        capture.write(100);

        assertEquals(limit, capture.bufferCapacity());
        assertEquals(limit, capture.size());
        assertEquals(99, capture.toByteArray()[limit - 1]);
        assertEquals(limit + 1L, capture.observed());
        assertTrue(capture.isTruncated());
    }

    @Test
    void shouldNotOverflowWhenCapDisabled() throws Exception {
        org.apache.commons.exec.CommandLine cmd =
            org.apache.commons.exec.CommandLine.parse("printf aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            cmd, null, null, 10_000L, 0L);
        SubprocessExecutionSupport.RunSession session = SubprocessExecutionSupport.execute(request);
        assertEquals(40, session.getStdout().toByteArray().length);
        assertFalse(session.isStdoutOverflowed());
    }

    @Test
    void shouldBuildExecutionRequest() {
        org.apache.commons.exec.CommandLine cmd = org.apache.commons.exec.CommandLine.parse("echo");
        SubprocessExecutionSupport.ExecutionRequest req = new SubprocessExecutionSupport.ExecutionRequest(
            cmd, null, null, 5000L);
        assertNotNull(req.getCommandLine());
        assertNull(req.getWorkingDirectory());
        assertNull(req.getEnvironment());
        assertEquals(5000L, req.getTimeoutMillis());
    }
}
