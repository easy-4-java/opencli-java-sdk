package io.github.easy4j.opencli.parser;

import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.JsonNode;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.core.OpenCliTypedResult;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenCliStdoutJsonTest {

    @Test
    void shouldParseValidJsonObject() {
        JsonNode node = OpenCliStdoutJson.parseLenient("{\"a\":1}");
        assertTrue(node.isObject());
        assertEquals(1, node.get("a").asInt());
    }

    @Test
    void shouldParseValidJsonArray() {
        JsonNode node = OpenCliStdoutJson.parseLenient("[1,2,3]");
        assertTrue(node.isArray());
        assertEquals(3, node.size());
    }

    @Test
    void shouldWrapNonJsonAsTextNode() {
        JsonNode node = OpenCliStdoutJson.parseLenient("not json at all");
        assertTrue(node.isTextual());
        assertEquals("not json at all", node.textValue());
    }

    @Test
    void debugDiagnosticDoesNotExposeParserInput() throws Exception {
        String canary = "stdoutsecret" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        JsonNode node;
        try (PrintStream capture = new PrintStream(diagnostics, true, "UTF-8")) {
            System.setErr(capture);
            try {
                node = OpenCliStdoutJson.parseLenient(canary);
            } finally {
                System.setErr(previousErr);
            }
        }

        assertEquals(canary, node.textValue());
        String logText = diagnostics.toString(StandardCharsets.UTF_8.name());
        assertTrue(logText.contains("stdout is not strict JSON"));
        assertFalse(logText.contains(canary));
    }

    @Test
    void shouldReturnEmptyTextNodeForNull() {
        JsonNode node = OpenCliStdoutJson.parseLenient(null);
        assertTrue(node.isTextual());
        assertEquals("", node.textValue());
    }

    @Test
    void shouldReturnEmptyTextNodeForEmpty() {
        JsonNode node = OpenCliStdoutJson.parseLenient("");
        assertTrue(node.isTextual());
        assertEquals("", node.textValue());
    }

    @Test
    void shouldCreateTypedResult() {
        OpenCliResult raw = OpenCliResult.builder().stdout("{\"x\":42}").success(true).build();
        OpenCliTypedResult<JsonNode> typed = OpenCliStdoutJson.typed(raw);
        assertSame(raw, typed.getRaw());
        assertTrue(typed.getStructured().isObject());
        assertEquals(42, typed.getStructured().get("x").asInt());
    }

    @Test
    void shouldCreateTypedResultForNonJson() {
        OpenCliResult raw = OpenCliResult.builder().stdout("plain text").success(true).build();
        OpenCliTypedResult<JsonNode> typed = OpenCliStdoutJson.typed(raw);
        assertTrue(typed.getStructured().isTextual());
    }
}
