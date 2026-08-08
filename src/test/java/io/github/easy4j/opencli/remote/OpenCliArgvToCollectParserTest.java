package io.github.easy4j.opencli.remote;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliArgvToCollectParserTest {

    @Test
    void shouldParseBasicArgv() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("chatgpt", "ask", "hello"), "json", "cdp", "");
        assertEquals("chatgpt", req.getSite());
        assertEquals("ask", req.getCommand());
        assertEquals("json", req.getFormat());
        assertEquals("cdp", req.getMode());
        assertTrue(req.getPositionalArgs().contains("hello"));
    }

    @Test
    void shouldParseKeyValueOptions() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "react", "--limit", "10"), "json", "cdp", "");
        assertEquals("react", req.getPositionalArgs().get(0));
        assertEquals("10", req.getArgs().get("limit"));
    }

    @Test
    void shouldParseEqualsOptions() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "--limit=10"), "json", "cdp", "");
        assertEquals("10", req.getArgs().get("limit"));
    }

    @Test
    void shouldParseBooleanFlags() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "react", "--verbose"), "json", "cdp", "");
        assertEquals(Boolean.TRUE, req.getArgs().get("verbose"));
    }

    @Test
    void shouldConsumeDashF() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "react", "-f", "yaml"), "json", "cdp", "");
        assertEquals("yaml", req.getFormat());
    }

    @Test
    void shouldConsumeDashDashFormat() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "react", "--format", "csv"), "json", "cdp", "");
        assertEquals("csv", req.getFormat());
    }

    @Test
    void shouldSetCdpEndpoint() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("chatgpt", "ask", "hi"), "json", "cdp", "ws://cdp:9222");
        assertEquals("ws://cdp:9222", req.getCdpEndpoint());
    }

    @Test
    void shouldThrowForTooFewTokens() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliArgvToCollectParser.parse(Arrays.asList("only-one"), "json", "cdp", ""));
    }

    @Test
    void shouldThrowForMissingDashFValue() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliArgvToCollectParser.parse(Arrays.asList("npm", "search", "-f"), "json", "cdp", ""));
    }

    @Test
    void shouldUseDefaultFormatWhenBlank() {
        OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(
            Arrays.asList("npm", "search", "r"), "", "", "");
        assertEquals("json", req.getFormat());
        assertEquals("cdp", req.getMode());
    }
}
