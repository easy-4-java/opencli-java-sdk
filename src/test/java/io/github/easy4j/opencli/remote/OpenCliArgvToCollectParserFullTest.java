package io.github.easy4j.opencli.remote;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliArgvToCollectParserFullTest {

    @Test
    void shouldRejectAnOptionWithoutAValueBeforeAnotherOption() {
        assertThrows(IllegalArgumentException.class, () -> OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "react", "--limit", "--verbose"), "json", "cdp", ""));
    }

    @Test
    void shouldParseEqualsWithFormatKey() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "r", "--format=yaml"), "json", "cdp", "");
        assertEquals("yaml", req.getFormat());
    }

    @Test
    void shouldParseDashDashFormatEquals() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "r", "--f=yaml"), "json", "cdp", "");
        assertEquals("yaml", req.getFormat());
    }

    @Test
    void shouldRejectNullTokens() {
        assertThrows(IllegalArgumentException.class, () -> OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", null, "react"), "json", "cdp", ""));
    }

    @Test
    void shouldRejectEmptyTokens() {
        assertThrows(IllegalArgumentException.class, () -> OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "", "react"), "json", "cdp", ""));
    }

    @Test
    void shouldThrowForMissingDashDashFormatValue() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliArgvToCollectParser.parse(Arrays.asList("npm", "search", "--format"), "json", "cdp", ""));
    }

    @Test
    void shouldThrowForEmptyOptionKey() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliArgvToCollectParser.parse(Arrays.asList("npm", "search", "--=value"), "json", "cdp", ""));
    }

    @Test
    void shouldParseMultiplePositionals() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("chatgpt", "send", "hello", "world"), "json", "cdp", "");
        assertEquals(Arrays.asList("hello", "world"), req.getPositionalArgs());
    }

    @Test
    void shouldUseDefaultModeWhenBlank() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "r"), "json", "", "");
        assertEquals("cdp", req.getMode());
    }
}
