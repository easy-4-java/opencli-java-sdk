package io.github.easy4j.opencli.core.availability;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import org.junit.jupiter.api.Test;
import java.util.Optional;

class OpenCliAvailabilityCheckerTest {

    private final OpenCliAvailabilityChecker checker = new OpenCliAvailabilityChecker();

    @Test
    void shouldSkipProbeForRemoteMode() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        OpenCliAvailabilityReport report = checker.check(props);
        assertEquals(OpenCliAvailabilityStatus.SKIPPED_REMOTE_MODE, report.getStatus());
        assertTrue(report.isAvailable());
    }

    @Test
    void shouldReportExecutableNotConfigured() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("   ");
        OpenCliAvailabilityReport report = checker.check(props);
        assertEquals(OpenCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED, report.getStatus());
        assertFalse(report.isAvailable());
    }

    @Test
    void shouldReportExecutableNotFound() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("/nonexistent/opencli");
        props.setStartupProbeTimeoutMillis(2000L);
        OpenCliAvailabilityReport report = checker.check(props);
        assertEquals(OpenCliAvailabilityStatus.EXECUTABLE_NOT_FOUND, report.getStatus());
        assertFalse(report.isAvailable());
    }

    @Test
    void shouldCheckWithExecutor() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        OpenCliExecutor executor = new OpenCliExecutor(props);
        OpenCliAvailabilityReport report = checker.check(executor);
        assertEquals(OpenCliAvailabilityStatus.SKIPPED_REMOTE_MODE, report.getStatus());
    }

    @Test
    void shouldRejectNullExecutor() {
        assertThrows(NullPointerException.class, () -> checker.check((OpenCliExecutor) null));
    }

    @Test
    void shouldRejectNullProperties() {
        assertThrows(NullPointerException.class, () -> checker.check((OpenCliProperties) null));
    }

    @Test
    void shouldResolvePathExecutable() {
        Optional<String> result = OpenCliAvailabilityChecker.resolveExecutablePath("/bin/ls");
        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnEmptyForNonexistentPath() {
        Optional<String> result = OpenCliAvailabilityChecker.resolveExecutablePath("/nonexistent/binary");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyForBlankInput() {
        Optional<String> result = OpenCliAvailabilityChecker.resolveExecutablePath("   ");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyForNullInput() {
        Optional<String> result = OpenCliAvailabilityChecker.resolveExecutablePath(null);
        assertFalse(result.isPresent());
    }
}
