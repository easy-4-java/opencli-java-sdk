package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenCliDiagnosticExportTest {

    @Test
    void explicitProjectionExcludesPayloadsAndLeavesResultIntact() {
        String sensitive = "secret-" + UUID.randomUUID();
        String stderr = "/trace/" + sensitive;
        String rawHttp = "https://user:" + sensitive + "@example.test";
        OpenCliParsedFields parsed = OpenCliParsedFields.builder().jsonShapeHint(sensitive).build();
        OpenCliResult result = OpenCliResult.builder()
            .success(false).exitCode(7).stdout(sensitive).stderr(stderr).remoteRawHttpBody(rawHttp).parsed(parsed).build();

        String export = OpenCliDiagnosticExport.export(result, 512);

        assertTrue(export.startsWith("policy=metadata-only\ntruncated=false\n"));
        assertTrue(export.contains("success=false\n"));
        assertTrue(export.contains("exitCode=7\n"));
        assertTrue(export.contains("stdoutChars=" + sensitive.length() + "\n"));
        assertTrue(export.contains("stderrChars=" + stderr.length() + "\n"));
        assertTrue(export.contains("rawHttpChars=" + rawHttp.length() + "\n"));
        assertFalse(export.contains(sensitive));
        assertEquals(sensitive, result.getStdout());
        assertEquals(stderr, result.getStderr());
        assertEquals(rawHttp, result.getRemoteRawHttpBody());
        assertEquals(sensitive, result.getParsed().getJsonShapeHint());
    }

    @Test
    void byteLimitUsesWholeFieldsAndReportsTruncation() {
        OpenCliResult result = OpenCliResult.builder().success(true).stdout("非 ASCII 内容").build();
        int maxBytes = 50;

        String export = OpenCliDiagnosticExport.export(result, maxBytes);

        assertTrue(export.getBytes(StandardCharsets.UTF_8).length <= maxBytes);
        assertEquals("policy=metadata-only\ntruncated=true\nsuccess=true\n", export);
        assertEquals("非 ASCII 内容", result.getStdout());
    }

    @Test
    void exactFitAndOneByteShortUseConsistentTruncationMarkers() {
        OpenCliResult result = OpenCliResult.builder().success(true).build();
        String expected = "policy=metadata-only\ntruncated=false\nsuccess=true\n";

        assertEquals(expected, OpenCliDiagnosticExport.export(result, expected.length()));
        assertEquals("policy=metadata-only\ntruncated=true\n",
            OpenCliDiagnosticExport.export(result, expected.length() - 1));
    }

    @Test
    void invalidBudgetDoesNotMutateResult() {
        OpenCliResult result = OpenCliResult.builder().success(false).stderr("original failure").build();

        assertThrows(IllegalArgumentException.class, () -> OpenCliDiagnosticExport.export(result, 1));
        assertEquals("original failure", result.getStderr());
        assertFalse(result.isSuccess());
    }

    @Test
    void absentValuesAreOmittedRatherThanReportedAsEmptyData() {
        String export = OpenCliDiagnosticExport.export(OpenCliResult.builder().stdout("").build(), 512);
        assertTrue(export.contains("stdoutChars=0\n"));
        assertFalse(export.contains("stderrChars"));
        assertFalse(export.contains("rawHttpChars"));
        assertFalse(export.contains("exitCode"));
    }
}
