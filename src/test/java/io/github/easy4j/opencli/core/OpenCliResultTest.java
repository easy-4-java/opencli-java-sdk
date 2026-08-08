package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import org.junit.jupiter.api.Test;

class OpenCliResultTest {

    @Test
    void shouldBuildResultWithAllFields() {
        OpenCliParsedFields parsed = OpenCliParsedFields.builder().jsonShapeHint("object").build();
        OpenCliResult r = OpenCliResult.builder()
            .stdout("{\"ok\":true}")
            .stderr("")
            .exitCode(0)
            .success(true)
            .parsed(parsed)
            .remoteRawHttpBody("raw")
            .build();
        assertEquals("{\"ok\":true}", r.getStdout());
        assertEquals("", r.getStderr());
        assertEquals(0, r.getExitCode());
        assertTrue(r.isSuccess());
        assertNotNull(r.getParsed());
        assertEquals("object", r.getParsed().getJsonShapeHint());
        assertEquals("raw", r.getRemoteRawHttpBody());
    }

    @Test
    void shouldBuildFailedResult() {
        OpenCliResult r = OpenCliResult.builder()
            .stdout("")
            .stderr("error")
            .exitCode(1)
            .success(false)
            .build();
        assertFalse(r.isSuccess());
        assertEquals(1, r.getExitCode());
        assertNull(r.getParsed());
        assertNull(r.getRemoteRawHttpBody());
    }
}
