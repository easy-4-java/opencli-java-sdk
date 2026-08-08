package io.github.easy4j.opencli.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliStringsTest {

    @Test
    void shouldReturnTrueForNull() {
        assertTrue(OpenCliStrings.isBlank(null));
    }

    @Test
    void shouldReturnTrueForEmptyString() {
        assertTrue(OpenCliStrings.isBlank(""));
    }

    @Test
    void shouldReturnTrueForBlankString() {
        assertTrue(OpenCliStrings.isBlank("   "));
    }

    @Test
    void shouldReturnFalseForNonBlankString() {
        assertFalse(OpenCliStrings.isBlank("hello"));
    }

    @Test
    void shouldReturnFalseForStringWithLeadingTrailingSpaces() {
        assertFalse(OpenCliStrings.isBlank("  hello  "));
    }

    @Test
    void shouldReturnOppositeOfIsBlank() {
        assertTrue(OpenCliStrings.isNotBlank("hello"));
        assertFalse(OpenCliStrings.isNotBlank(null));
        assertFalse(OpenCliStrings.isNotBlank(""));
        assertFalse(OpenCliStrings.isNotBlank("   "));
    }
}
