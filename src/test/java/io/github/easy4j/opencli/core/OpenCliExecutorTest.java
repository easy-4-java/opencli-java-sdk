package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenCliExecutorTest {

    @Test
    void shouldRejectNullProperties() {
        assertThrows(NullPointerException.class, () -> new OpenCliExecutor(null));
    }

    @Test
    void shouldStoreProperties() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertSame(props, executor.getProperties());
    }

    @Test
    void shouldAppendQuotedKeyValue() {
        CommandLine cmd = new CommandLine("opencli");
        OpenCliExecutor.appendQuotedKeyValue(cmd, "--key", "value");
        String[] args = cmd.getArguments();
        assertTrue(args.length > 0);
        assertTrue(args[args.length - 1].contains("--key=value"));
    }

    @Test
    void shouldRejectKeyWithoutDoubleDash() {
        CommandLine cmd = new CommandLine("opencli");
        assertThrows(IllegalArgumentException.class, () -> OpenCliExecutor.appendQuotedKeyValue(cmd, "key", "value"));
    }

    @Test
    void shouldAppendQuotedKeyValueStrippingTrailingEquals() {
        CommandLine cmd = new CommandLine("opencli");
        OpenCliExecutor.appendQuotedKeyValue(cmd, "--key=", "value");
        String[] args = cmd.getArguments();
        assertTrue(args[args.length - 1].contains("--key=value"));
    }

    @Test
    void shouldThrowWhenInvokingWithEmptyArgv() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(IllegalArgumentException.class, () -> executor.invoke(java.util.Collections.emptyList()));
    }

    @Test
    void shouldThrowWhenExecutableIsBlank() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("   ");
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(IllegalStateException.class, () -> executor.invoke("list"));
    }

    @Test
    void shouldThrowWhenTimeoutIsZero() {
        OpenCliProperties props = new OpenCliProperties();
        props.setCommandTimeoutMillis(0);
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(IllegalStateException.class, () -> executor.invoke("list"));
    }

    @Test
    void shouldThrowWhenWorkingDirectoryDoesNotExist() {
        OpenCliProperties props = new OpenCliProperties();
        props.setWorkingDirectory("/nonexistent/path/xyz");
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(OpenCliExecutableFailureException.class, () -> executor.invoke("list"));
    }
    @Test
    void spawnFailureStackTraceOmitsExecutablePath(@TempDir Path directory) {
        String canary = "private-executable-path-canary";
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable(directory.resolve(canary).toString());

        try (OpenCliExecutor executor = new OpenCliExecutor(props)) {
            OpenCliExecutableFailureException failure = assertThrows(
                OpenCliExecutableFailureException.class, () -> executor.invoke("list"));
            StringWriter diagnostics = new StringWriter();
            failure.printStackTrace(new PrintWriter(diagnostics));
            assertFalse(diagnostics.toString().contains(canary));
            assertEquals(OpenCliExecutionDetails.TerminationReason.SPAWN_FAILED,
                failure.getPartialResult().getExecutionDetails().getTerminationReason());
            assertNull(failure.getPartialResult().getExitCode());
        }
    }
}
