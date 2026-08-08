package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliCenterWsCollectToArgvTest {

    @Test
    void shouldConvertBasicFields() {
        List<String> argv = OpenCliCenterWsCollectToArgv.toArgv("chatgpt", "ask", null, null, "json");
        assertTrue(argv.contains("chatgpt"));
        assertTrue(argv.contains("ask"));
        assertTrue(argv.contains("-f"));
        assertTrue(argv.contains("json"));
    }

    @Test
    void shouldIncludePositionalArgs() {
        List<String> positional = Arrays.asList("hello", "world");
        List<String> argv = OpenCliCenterWsCollectToArgv.toArgv("chatgpt", "ask", positional, null, "json");
        assertTrue(argv.contains("hello"));
        assertTrue(argv.contains("world"));
    }

    @Test
    void shouldIncludeArgsMap() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("timeout", "30");
        args.put("verbose", true);
        List<String> argv = OpenCliCenterWsCollectToArgv.toArgv("npm", "search", null, args, "json");
        assertTrue(argv.contains("--timeout"));
        assertTrue(argv.contains("30"));
        assertTrue(argv.contains("--verbose"));
    }

    @Test
    void shouldUseDefaultFormatWhenBlank() {
        List<String> argv = OpenCliCenterWsCollectToArgv.toArgv("npm", "search", null, null, "");
        assertTrue(argv.contains("json"));
    }

    @Test
    void shouldThrowForBlankSite() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCenterWsCollectToArgv.toArgv("", "ask", null, null, "json"));
    }

    @Test
    void shouldThrowForBlankCommand() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCenterWsCollectToArgv.toArgv("chatgpt", "", null, null, "json"));
    }

    @Test
    void shouldFilterNullPositionals() {
        List<String> positional = Arrays.asList("a", null, "", "b");
        List<String> argv = OpenCliCenterWsCollectToArgv.toArgv("c", "d", positional, null, "json");
        assertTrue(argv.contains("a"));
        assertTrue(argv.contains("b"));
    }

    @Test
    void shouldSkipNullArgKeys() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put(null, "val");
        args.put("", "val2");
        List<String> argv = OpenCliCenterWsCollectToArgv.toArgv("x", "y", null, args, "json");
        assertFalse(argv.contains("--null"));
    }
}
