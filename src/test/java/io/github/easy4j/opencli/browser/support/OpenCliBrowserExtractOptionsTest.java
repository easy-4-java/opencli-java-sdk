package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserExtractOptionsTest {

    @Test
    void shouldAppendOptions() {
        OpenCliBrowserExtractOptions opts = OpenCliBrowserExtractOptions.builder()
            .selector("main")
            .chunkSize(5000)
            .start(100)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--selector"));
        assertTrue(target.contains("main"));
        assertTrue(target.contains("--chunk-size"));
        assertTrue(target.contains("5000"));
        assertTrue(target.contains("--start"));
        assertTrue(target.contains("100"));
    }
}
