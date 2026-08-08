package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenCliArgSupportTest {

    @Test
    void shouldMergeNonNullLists() {
        List<String> result = OpenCliArgSupport.merge(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    void shouldFilterNullAndBlankWhenMerging() {
        List<String> result = OpenCliArgSupport.merge(Arrays.asList("a", null, "", "  ", "b"), Arrays.asList("c"));
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    void shouldHandleNullPrefix() {
        List<String> result = OpenCliArgSupport.merge(null, Arrays.asList("a"));
        assertEquals(Arrays.asList("a"), result);
    }

    @Test
    void shouldHandleNullAdditional() {
        List<String> result = OpenCliArgSupport.merge(Arrays.asList("a"), null);
        assertEquals(Arrays.asList("a"), result);
    }

    @Test
    void shouldHandleBothNull() {
        List<String> result = OpenCliArgSupport.merge(null, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldAddOptionEquals() {
        List<String> target = new ArrayList<>();
        OpenCliArgSupport.addOptionEquals(target, "--key", "value");
        assertEquals(Arrays.asList("--key=value"), target);
    }

    @Test
    void shouldAddOptionEqualsStrippingTrailingEquals() {
        List<String> target = new ArrayList<>();
        OpenCliArgSupport.addOptionEquals(target, "--key=", "value");
        assertEquals(Arrays.asList("--key=value"), target);
    }

    @Test
    void shouldThrowWhenNameDoesNotStartWithDash() {
        List<String> target = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> OpenCliArgSupport.addOptionEquals(target, "key", "value"));
    }

    @Test
    void shouldAddOptionPair() {
        List<String> target = new ArrayList<>();
        OpenCliArgSupport.addOptionPair(target, "--flag", "val");
        assertEquals(Arrays.asList("--flag", "val"), target);
    }

    @Test
    void shouldAddOptionPairIfPresentWhenValueNotNull() {
        List<String> target = new ArrayList<>();
        OpenCliArgSupport.addOptionPairIfPresent(target, "--flag", "val");
        assertEquals(Arrays.asList("--flag", "val"), target);
    }

    @Test
    void shouldNotAddOptionPairIfPresentWhenValueNull() {
        List<String> target = new ArrayList<>();
        OpenCliArgSupport.addOptionPairIfPresent(target, "--flag", null);
        assertTrue(target.isEmpty());
    }

    @Test
    void shouldAddFlagIfTrue() {
        List<String> target = new ArrayList<>();
        OpenCliArgSupport.addFlagIfTrue(target, "--verbose", true);
        assertEquals(Arrays.asList("--verbose"), target);
    }

    @Test
    void shouldNotAddFlagIfFalse() {
        List<String> target = new ArrayList<>();
        OpenCliArgSupport.addFlagIfTrue(target, "--verbose", false);
        assertTrue(target.isEmpty());
    }

    @Test
    void shouldNotAddFlagIfNull() {
        List<String> target = new ArrayList<>();
        OpenCliArgSupport.addFlagIfTrue(target, "--verbose", null);
        assertTrue(target.isEmpty());
    }
}
