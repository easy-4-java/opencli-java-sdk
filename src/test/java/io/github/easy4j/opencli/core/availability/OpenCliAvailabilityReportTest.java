package io.github.easy4j.opencli.core.availability;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliAvailabilityReportTest {

    @Test
    void shouldBuildReport() {
        OpenCliAvailabilityReport report = OpenCliAvailabilityReport.builder()
            .status(OpenCliAvailabilityStatus.AVAILABLE)
            .available(true)
            .configuredExecutable("opencli")
            .resolvedExecutablePath("/usr/bin/opencli")
            .message("ok")
            .build();
        assertEquals(OpenCliAvailabilityStatus.AVAILABLE, report.getStatus());
        assertTrue(report.isAvailable());
        assertEquals("opencli", report.getConfiguredExecutable());
        assertEquals("/usr/bin/opencli", report.getResolvedExecutablePath());
        assertEquals("ok", report.getMessage());
        assertNull(report.getProbeResult());
    }

    @Test
    void shouldKeepPathsAndProviderDetailsOutOfDiagnosticMessage() {
        OpenCliAvailabilityReport report = OpenCliAvailabilityReport.builder()
            .status(OpenCliAvailabilityStatus.EXECUTABLE_NOT_FOUND)
            .available(false)
            .configuredExecutable("private-configured-executable")
            .resolvedExecutablePath("/private/resolved-executable")
            .message("private-probe-detail")
            .build();
        String diag = report.toDiagnosticMessage();
        assertTrue(diag.contains("unavailable"));
        assertTrue(diag.contains("EXECUTABLE_NOT_FOUND"));
        assertFalse(diag.contains("private"));
        assertEquals("private-configured-executable", report.getConfiguredExecutable());
        assertEquals("/private/resolved-executable", report.getResolvedExecutablePath());
        assertEquals("private-probe-detail", report.getMessage());
    }

    @Test
    void shouldGenerateDiagnosticForAvailable() {
        OpenCliAvailabilityReport report = OpenCliAvailabilityReport.builder()
            .status(OpenCliAvailabilityStatus.AVAILABLE)
            .available(true)
            .configuredExecutable("opencli")
            .message("ok")
            .build();
        String diag = report.toDiagnosticMessage();
        assertTrue(diag.contains("ready"));
    }
}
