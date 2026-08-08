package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserConsoleOptionsTest {

    @Test
    void shouldAppendAllOptions() {
        OpenCliBrowserConsoleOptions opts = OpenCliBrowserConsoleOptions.builder()
            .level("error")
            .since("30s")
            .until("5m")
            .follow(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--level"));
        assertTrue(target.contains("error"));
        assertTrue(target.contains("--since"));
        assertTrue(target.contains("30s"));
        assertTrue(target.contains("--until"));
        assertTrue(target.contains("5m"));
        assertTrue(target.contains("--follow"));
    }

    @Test
    void shouldNotAppendNullOptions() {
        OpenCliBrowserConsoleOptions opts = OpenCliBrowserConsoleOptions.builder().build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.isEmpty());
    }
}
