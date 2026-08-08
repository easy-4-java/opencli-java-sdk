package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserScreenshotOptionsTest {

    @Test
    void shouldAppendAllOptions() {
        OpenCliBrowserScreenshotOptions opts = OpenCliBrowserScreenshotOptions.builder()
            .annotate(true)
            .fullPage(true)
            .width(1920)
            .height(1080)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--annotate"));
        assertTrue(target.contains("--full-page"));
        assertTrue(target.contains("--width"));
        assertTrue(target.contains("1920"));
        assertTrue(target.contains("--height"));
        assertTrue(target.contains("1080"));
    }
}
