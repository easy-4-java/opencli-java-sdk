package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserTabOptionsTest {

    @Test
    void shouldAppendTab() {
        OpenCliBrowserTabOptions opts = OpenCliBrowserTabOptions.builder().tab("tab-1").build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertEquals(Arrays.asList("--tab", "tab-1"), target);
    }

    @Test
    void shouldNotAppendNullTab() {
        OpenCliBrowserTabOptions opts = OpenCliBrowserTabOptions.builder().build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.isEmpty());
    }
}
