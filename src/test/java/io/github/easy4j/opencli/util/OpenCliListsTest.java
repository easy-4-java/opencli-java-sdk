package io.github.easy4j.opencli.util;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenCliListsTest {

    @Test
    void shouldReturnEmptyListForNull() {
        List<String> result = OpenCliLists.of((String[]) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForNoArgs() {
        List<String> result = OpenCliLists.of();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnListWithSingleElement() {
        List<String> result = OpenCliLists.of("hello");
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0));
    }

    @Test
    void shouldReturnListWithMultipleElements() {
        List<String> result = OpenCliLists.of("a", "b", "c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    void shouldReturnImmutableList() {
        List<String> result = OpenCliLists.of("a", "b");
        assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
    }
}
