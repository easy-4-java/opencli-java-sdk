package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserFindOptionsTest {

    @Test
    void shouldAppendOptions() {
        OpenCliBrowserFindOptions opts = OpenCliBrowserFindOptions.builder()
            .css(".btn")
            .limit(10)
            .textMax(200)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--css"));
        assertTrue(target.contains(".btn"));
        assertTrue(target.contains("--limit"));
        assertTrue(target.contains("10"));
        assertTrue(target.contains("--text-max"));
        assertTrue(target.contains("200"));
    }

    @Test
    void shouldSupportToBuilder() {
        OpenCliBrowserFindOptions opts = OpenCliBrowserFindOptions.builder().css(".btn").build();
        OpenCliBrowserFindOptions copy = opts.toBuilder().limit(5).build();
        assertEquals(".btn", copy.getCss());
        assertEquals(5, copy.getLimit());
    }
}
