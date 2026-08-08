package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliAdapterCommandRequestTest {

    @Test
    void shouldBuildWithPositionals() {
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("ask")
            .positional("hello")
            .positional("world")
            .build();
        assertEquals("ask", req.getSubcommand());
        assertEquals(Arrays.asList("hello", "world"), req.getPositionals());
        assertTrue(req.getOptions().isEmpty());
    }

    @Test
    void shouldBuildWithOptions() {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("--timeout", "30");
        opts.put("--verbose", true);
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("ask")
            .options(opts)
            .build();
        Map<String, Object> result = req.getOptions();
        assertEquals("30", result.get("--timeout"));
        assertEquals(true, result.get("--verbose"));
    }

    @Test
    void shouldConvertToSubcommandAndArgs() {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("--timeout", "30");
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("ask")
            .positional("hello")
            .options(opts)
            .build();
        List<String> argv = req.toSubcommandAndArgs();
        assertEquals("ask", argv.get(0));
        assertEquals("hello", argv.get(1));
        assertTrue(argv.contains("--timeout"));
        assertTrue(argv.contains("30"));
    }

    @Test
    void shouldTreatBooleanTrueAsFlag() {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("--verbose", true);
        opts.put("--quiet", false);
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("search")
            .options(opts)
            .build();
        List<String> argv = req.toSubcommandAndArgs();
        assertTrue(argv.contains("--verbose"));
        assertFalse(argv.contains("--quiet"));
    }

    @Test
    void shouldReturnEmptyPositionalsWhenNull() {
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("list")
            .build();
        assertNotNull(req.getPositionals());
        assertTrue(req.getPositionals().isEmpty());
    }

    @Test
    void shouldReturnEmptyOptionsWhenNull() {
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("list")
            .build();
        assertNotNull(req.getOptions());
        assertTrue(req.getOptions().isEmpty());
    }

    @Test
    void shouldCreateFromStaticFactory() {
        List<String> pos = Arrays.asList("arg1");
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("--flag", "val");
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.of("sub", pos, opts);
        assertEquals("sub", req.getSubcommand());
        assertEquals(Arrays.asList("arg1"), req.getPositionals());
        assertEquals("val", req.getOptions().get("--flag"));
    }

    @Test
    void shouldCreateFromStaticFactoryWithNulls() {
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.of("sub", null, null);
        assertEquals("sub", req.getSubcommand());
        assertTrue(req.getPositionals().isEmpty());
        assertTrue(req.getOptions().isEmpty());
    }

    @Test
    void shouldReturnImmutablePositionals() {
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("sub").positional("a").build();
        assertThrows(UnsupportedOperationException.class, () -> req.getPositionals().add("b"));
    }

    @Test
    void shouldReturnImmutableOptions() {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("--k", "v");
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("sub").options(opts).build();
        assertThrows(UnsupportedOperationException.class, () -> req.getOptions().put("--x", "y"));
    }

    @Test
    void shouldSkipNullPositionalInToSubcommandAndArgs() {
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("sub")
            .positional("a")
            .positional("")
            .positional("b")
            .build();
        List<String> argv = req.toSubcommandAndArgs();
        assertEquals(Arrays.asList("sub", "a", "b"), argv);
    }

    @Test
    void shouldSkipNullOptionValue() {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("--key", null);
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("sub")
            .options(opts)
            .build();
        List<String> argv = req.toSubcommandAndArgs();
        assertFalse(argv.contains("--key"));
    }
}
