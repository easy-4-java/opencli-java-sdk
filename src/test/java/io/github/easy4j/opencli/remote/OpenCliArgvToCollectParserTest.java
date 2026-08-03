package io.github.easy4j.opencli.remote;

import io.github.easy4j.opencli.util.OpenCliLists;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link OpenCliArgvToCollectParser} 单元测试。
 */
class OpenCliArgvToCollectParserTest {

    @Test
    void parsesPositionalAndNamedOptions() {
        OpenCliCollectRequest r =
            OpenCliArgvToCollectParser.parse(
                OpenCliLists.of("arxiv", "search", "attention", "--limit", "5"),
                "json",
                "cdp",
                "");
        assertEquals("arxiv", r.getSite());
        assertEquals("search", r.getCommand());
        assertEquals(OpenCliLists.of("attention"), r.getPositionalArgs());
        assertEquals("5", String.valueOf(r.getArgs().get("limit")));
        assertEquals("json", r.getFormat());
        assertEquals("cdp", r.getMode());
    }

    @Test
    void extractsFormatFromArgv() {
        OpenCliCollectRequest r =
            OpenCliArgvToCollectParser.parse(
                OpenCliLists.of("npm", "package", "react", "-f", "yaml"),
                "json",
                "bridge",
                "");
        assertEquals("yaml", r.getFormat());
        assertEquals("bridge", r.getMode());
    }

    @Test
    void requiresAdapterAndCommand() {
        assertThrows(
            IllegalArgumentException.class,
            () -> OpenCliArgvToCollectParser.parse(OpenCliLists.of("only"), "json", "cdp", ""));
    }
}
