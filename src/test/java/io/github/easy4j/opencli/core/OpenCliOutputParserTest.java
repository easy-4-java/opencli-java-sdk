package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import org.junit.jupiter.api.Test;

class OpenCliOutputParserTest {

    @Test
    void shouldDetectArrayShape() {
        OpenCliParsedFields f = OpenCliOutputParser.parseBestEffort("[1,2,3]", "");
        assertEquals("array", f.getJsonShapeHint());
    }

    @Test
    void shouldDetectObjectShape() {
        OpenCliParsedFields f = OpenCliOutputParser.parseBestEffort("{\"a\":1}", "");
        assertEquals("object", f.getJsonShapeHint());
    }

    @Test
    void shouldReturnNullHintForPlainText() {
        OpenCliParsedFields f = OpenCliOutputParser.parseBestEffort("hello world", "");
        assertNull(f.getJsonShapeHint());
    }

    @Test
    void shouldUseStderrWhenStdoutBlank() {
        OpenCliParsedFields f = OpenCliOutputParser.parseBestEffort("", "{\"err\":1}");
        assertEquals("object", f.getJsonShapeHint());
    }

    @Test
    void shouldHandleBothBlank() {
        OpenCliParsedFields f = OpenCliOutputParser.parseBestEffort("", "");
        assertNull(f.getJsonShapeHint());
    }

    @Test
    void shouldHandleNullStdout() {
        OpenCliParsedFields f = OpenCliOutputParser.parseBestEffort(null, null);
        assertNull(f.getJsonShapeHint());
    }
}
