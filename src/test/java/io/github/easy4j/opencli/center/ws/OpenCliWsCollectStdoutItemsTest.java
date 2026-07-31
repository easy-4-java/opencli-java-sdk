package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenCliWsCollectStdoutItemsTest {

    @Test
    void parsesJsonArray() {
        List<JsonNode> items =
            OpenCliWsCollectStdoutItems.parseItems("prefix [{\"a\":1},{\"b\":2}]", "json");
        assertEquals(2, items.size());
        assertEquals(1, items.get(0).path("a").asInt());
    }

    @Test
    void parsesSingleObjectAsOneRow() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("{\"x\":true}", "json");
        assertEquals(1, items.size());
        assertTrue(items.get(0).path("x").asBoolean());
    }

    @Test
    void nonJsonFormatBecomesContentRow() {
        List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems("plain", "text");
        assertEquals(1, items.size());
        assertEquals("plain", items.get(0).path("content").asText());
    }
}
