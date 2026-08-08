package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenCliWsCollectStdoutItemsTest {

    @Test
    void shouldParseJsonArray() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("[{\"a\":1},{\"b\":2}]", "json");
        assertEquals(2, items.size());
    }

    @Test
    void shouldParseJsonObject() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("{\"key\":\"val\"}", "json");
        assertEquals(1, items.size());
        assertEquals("val", items.get(0).get("key").asText());
    }

    @Test
    void shouldReturnEmptyForNullStdout() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems(null, "json");
        assertTrue(items.isEmpty());
    }

    @Test
    void shouldReturnEmptyForEmptyStdout() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("", "json");
        assertTrue(items.isEmpty());
    }

    @Test
    void shouldReturnEmptyForInvalidJson() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("not json", "json");
        assertTrue(items.isEmpty());
    }

    @Test
    void shouldWrapNonJsonFormatAsSingleContentItem() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("plain text", "text");
        assertEquals(1, items.size());
        assertEquals("plain text", items.get(0).get("content").asText());
    }

    @Test
    void shouldUseJsonAsDefaultFormat() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("{\"x\":1}", "");
        assertEquals(1, items.size());
    }

    @Test
    void shouldHandleJsonWithLeadingWhitespace() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("  \n  {\"x\":1}  ", "json");
        assertEquals(1, items.size());
    }

    @Test
    void shouldHandleJsonArrayWrappedInText() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("prefix [1,2,3] suffix", "json");
        assertEquals(3, items.size());
    }
}
