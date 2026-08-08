package io.github.easy4j.opencli.parser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliParsedFieldsTest {

    @Test
    void shouldBuildWithJsonShapeHint() {
        OpenCliParsedFields f = OpenCliParsedFields.builder().jsonShapeHint("array").build();
        assertEquals("array", f.getJsonShapeHint());
    }

    @Test
    void shouldBuildWithNullHint() {
        OpenCliParsedFields f = OpenCliParsedFields.builder().build();
        assertNull(f.getJsonShapeHint());
    }
}
