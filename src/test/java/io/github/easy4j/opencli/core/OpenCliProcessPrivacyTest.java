package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.contract.LifecycleProbe;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliTimeoutException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class OpenCliProcessPrivacyTest {

    @Test
    void keepsMissingExecutablePathOutOfLogsAndStackTrace(@TempDir Path directory) throws Exception {
        String canary = "executable-" + UUID.randomUUID();
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutable(directory.resolve(canary).toString());

        try (OpenCliExecutor executor = new OpenCliExecutor(properties)) {
            assertPrivateFailure(OpenCliException.class, canary, () -> executor.invoke("list"));
        }
    }

    @Test
    void preservesStderrOnNonzeroExitWithoutLeakingArgsOrStderr() throws Exception {
        String canary = "stderr-" + UUID.randomUUID();
        try (OpenCliExecutor executor = new OpenCliExecutor(childProperties())) {
            OpenCliNonZeroExitException failure = assertPrivateFailure(OpenCliNonZeroExitException.class, canary,
                () -> executor.invoke("privacy", "nonzero", canary));
            assertEquals(canary, failure.getPartialResult().getStderr());
            assertEquals(Integer.valueOf(7), failure.getPartialResult().getExitCode());
        }
    }

    @Test
    void correlatesLocalFailureLogAndStackWithoutPayload() throws Exception {
        String canary = "private-stderr-" + UUID.randomUUID();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.err;
        OpenCliNonZeroExitException failure;
        try (OpenCliExecutor executor = new OpenCliExecutor(childProperties());
             PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8.name())) {
            System.setErr(capture);
            try {
                failure = assertThrows(OpenCliNonZeroExitException.class,
                    () -> executor.invoke("privacy", "nonzero", canary));
                failure.printStackTrace(capture);
            } finally {
                System.setErr(original);
            }
        }

        String message = failure.getMessage();
        String marker = "correlationId=";
        int markerStart = message.indexOf(marker);
        assertTrue(markerStart >= 0, message);
        UUID id = UUID.fromString(message.substring(markerStart + marker.length()));
        String diagnostics = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(diagnostics.contains("OpenCLI process failed " + marker + id
            + " reason=PROCESS_EXIT exitCode=7 stdoutBytes=0 stderrBytes=" + canary.length()));
        assertFalse(diagnostics.contains(canary));
        assertEquals(canary, failure.getPartialResult().getStderr());
    }

    @Test
    void adapterDebugLogsOnlyArgumentCount() throws Exception {
        String canary = "adapter-value-" + UUID.randomUUID();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.err;
        try (OpenCliExecutor executor = new OpenCliExecutor(childProperties());
             PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8.name())) {
            System.setErr(capture);
            try {
                OpenCliAdapterChannel adapter = new OpenCliAdapterChannel(executor, "privacy");
                OpenCliNonZeroExitException failure = assertThrows(OpenCliNonZeroExitException.class,
                    () -> adapter.invoke("nonzero", canary));
                failure.printStackTrace(capture);
                assertEquals(canary, failure.getPartialResult().getStderr());
            } finally {
                System.setErr(original);
            }
        }
        String diagnostics = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(diagnostics.contains("OpenCLI adapter invoke argvSize=3"));
        assertFalse(diagnostics.contains(canary));
    }

    @Test
    void keepsArgumentsAndPartialStderrOutOfTimeoutDiagnostics() throws Exception {
        String canary = "timeout-" + UUID.randomUUID();
        OpenCliProperties properties = childProperties();
        properties.setCommandTimeoutMillis(2500);

        try (OpenCliExecutor executor = new OpenCliExecutor(properties)) {
            OpenCliTimeoutException failure = assertPrivateFailure(OpenCliTimeoutException.class, canary,
                () -> executor.invoke("privacy", "timeout", canary));
            assertEquals(canary, failure.getPartialResult().getStderr());
        }
    }

    @Test
    void keepsPartialStdoutOutOfOutputLimitDiagnostics() throws Exception {
        String canary = "overflow-" + UUID.randomUUID();
        OpenCliProperties properties = childProperties();
        properties.setMaxStdoutBytes(128);

        try (OpenCliExecutor executor = new OpenCliExecutor(properties)) {
            OpenCliException failure = assertPrivateFailure(OpenCliException.class, canary,
                () -> executor.invoke("privacy", "overflow", canary));
            assertEquals("OUTPUT_LIMIT", failure.getPartialResult().getExecutionDetails().getTerminationReason().name());
            assertEquals(128, failure.getPartialResult().getStdout().length());
            assertTrue(failure.getPartialResult().getStdout().startsWith(canary));
        }
    }

    private static OpenCliProperties childProperties() {
        OpenCliProperties properties = new OpenCliProperties();
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        properties.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath());
        properties.setLeadingArguments(new ArrayList<>(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            LifecycleProbe.class.getName())));
        return properties;
    }

    private static <T extends OpenCliException> T assertPrivateFailure(
        Class<T> type, String canary, Executable invocation) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.err;
        T failure;
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8.name())) {
            System.setErr(capture);
            failure = assertThrows(type, invocation);
            failure.printStackTrace(capture);
        } finally {
            System.setErr(original);
        }
        assertFalse(output.toString(StandardCharsets.UTF_8.name()).contains(canary));
        return failure;
    }
}
