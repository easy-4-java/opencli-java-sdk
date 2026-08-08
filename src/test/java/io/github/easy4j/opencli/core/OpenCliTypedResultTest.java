package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliTypedResultTest {

    @Test
    void shouldCreateTypedResultViaFactory() {
        OpenCliResult raw = OpenCliResult.builder().stdout("out").success(true).build();
        OpenCliTypedResult<String> typed = OpenCliTypedResult.of(raw, "structured");
        assertSame(raw, typed.getRaw());
        assertEquals("structured", typed.getStructured());
    }

    @Test
    void shouldCreateTypedResultViaConstructor() {
        OpenCliResult raw = OpenCliResult.builder().stdout("out").success(true).build();
        OpenCliTypedResult<Integer> typed = new OpenCliTypedResult<>(raw, 42);
        assertSame(raw, typed.getRaw());
        assertEquals(42, typed.getStructured());
    }
}
