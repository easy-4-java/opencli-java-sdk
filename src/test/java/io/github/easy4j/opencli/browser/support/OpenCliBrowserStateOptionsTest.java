package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserStateOptionsTest {

    @Test
    void shouldAppendOptions() {
        OpenCliBrowserStateOptions opts = OpenCliBrowserStateOptions.builder()
            .source("dom")
            .compareSources(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--source"));
        assertTrue(target.contains("dom"));
        assertTrue(target.contains("--compare-sources"));
    }
}
