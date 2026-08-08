package io.github.easy4j.opencli.core.availability;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliProperties;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OpenCliAvailabilityCheckerFullTest {

    private final OpenCliAvailabilityChecker checker = new OpenCliAvailabilityChecker();

    @Test
    void shouldResolvePathExecutableThatExists() {
        // /bin/sh should exist on macOS/Linux
        Optional<String> result = OpenCliAvailabilityChecker.resolveExecutablePath("/bin/sh");
        assertTrue(result.isPresent());
    }

    @Test
    void shouldNotResolveDirectory() {
        Optional<String> result = OpenCliAvailabilityChecker.resolveExecutablePath("/tmp");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldNotResolveNonexistentRelativePath() {
        Optional<String> result = OpenCliAvailabilityChecker.resolveExecutablePath("nonexistent_binary_xyz_12345");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReportNotExecutableForExistingNonExecutableFile() throws Exception {
        java.io.File tmp = java.io.File.createTempFile("test-nonexec", ".sh");
        tmp.setExecutable(false);
        tmp.deleteOnExit();
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable(tmp.getAbsolutePath());
        props.setStartupProbeTimeoutMillis(2000L);
        OpenCliAvailabilityReport report = checker.check(props);
        assertEquals(OpenCliAvailabilityStatus.EXECUTABLE_NOT_EXECUTABLE, report.getStatus());
        assertFalse(report.isAvailable());
    }

    @Test
    void shouldGenerateDiagnosticForSkippedRemote() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutionTarget(io.github.easy4j.opencli.OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        OpenCliAvailabilityReport report = checker.check(props);
        String diag = report.toDiagnosticMessage();
        assertTrue(diag.contains("ready"));
        assertTrue(diag.contains("SKIPPED_REMOTE_MODE"));
    }
}
