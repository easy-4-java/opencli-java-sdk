package io.github.easy4j.opencli.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/**
 * {@link OpenCliStdoutJson} 单元测试。
 */
class OpenCliStdoutJsonTest {

    @Test
    void parseLenientReadsObject() {
        JsonNode n = OpenCliStdoutJson.parseLenient("{\"a\":1}");
        assertTrue(n.isObject());
        assertEquals(1, n.path("a").asInt());
    }

    @Test
    void parseLenientWrapsNonJsonAsText() {
        JsonNode n = OpenCliStdoutJson.parseLenient("plain");
        assertTrue(n.isTextual());
        assertEquals("plain", n.asText());
    }
}
