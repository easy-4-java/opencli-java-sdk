package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliTimeoutException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void preservesStderrOnNonzeroExitWithoutLeakingArgsOrStderr() throws Exception {
        String canary = "stderr-" + UUID.randomUUID();
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutable("/bin/sh");

        try (OpenCliExecutor executor = new OpenCliExecutor(properties)) {
            OpenCliNonZeroExitException failure = assertPrivateFailure(OpenCliNonZeroExitException.class, canary,
                () -> executor.invoke("-c", "printf '%s' \"$1\" >&2; exit 7", "fixture", canary));
            assertEquals(canary, failure.getPartialResult().getStderr());
            assertEquals(Integer.valueOf(7), failure.getPartialResult().getExitCode());
        }
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void keepsArgumentsOutOfTimeoutDiagnostics() throws Exception {
        String canary = "timeout-" + UUID.randomUUID();
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutable("/bin/sh");
        properties.setCommandTimeoutMillis(200);

        try (OpenCliExecutor executor = new OpenCliExecutor(properties)) {
            assertPrivateFailure(OpenCliTimeoutException.class, canary,
                () -> executor.invoke("-c", "exec sleep 5", "fixture", canary));
        }
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
