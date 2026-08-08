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
    void shouldGenerateDiagnosticMessage() {
        OpenCliAvailabilityReport report = OpenCliAvailabilityReport.builder()
            .status(OpenCliAvailabilityStatus.EXECUTABLE_NOT_FOUND)
            .available(false)
            .configuredExecutable("opencli")
            .message("not found on PATH")
            .build();
        String diag = report.toDiagnosticMessage();
        assertTrue(diag.contains("unavailable"));
        assertTrue(diag.contains("EXECUTABLE_NOT_FOUND"));
        assertTrue(diag.contains("opencli"));
        assertTrue(diag.contains("not found on PATH"));
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
