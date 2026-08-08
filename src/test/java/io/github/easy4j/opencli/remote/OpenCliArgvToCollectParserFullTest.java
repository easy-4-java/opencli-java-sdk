package io.github.easy4j.opencli.remote;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliArgvToCollectParserFullTest {

    @Test
    void shouldParseDashDashFlagWithNextOption() {
        // --verbose followed by another flag means --verbose is boolean
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "react", "--verbose", "--limit", "5"), "json", "cdp", "");
        assertEquals(Boolean.TRUE, req.getArgs().get("verbose"));
        assertEquals("5", req.getArgs().get("limit"));
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
    void shouldSkipNullTokens() {
        List<String> argv = new ArrayList<>(Arrays.asList("npm", "search", null, "react"));
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(argv, "json", "cdp", "");
        assertTrue(req.getPositionalArgs().contains("react"));
    }

    @Test
    void shouldSkipEmptyTokens() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "", "react"), "json", "cdp", "");
        assertTrue(req.getPositionalArgs().contains("react"));
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
