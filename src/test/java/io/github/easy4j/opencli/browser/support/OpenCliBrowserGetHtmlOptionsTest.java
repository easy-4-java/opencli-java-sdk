package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserGetHtmlOptionsTest {

    @Test
    void shouldAppendOptions() {
        OpenCliBrowserGetHtmlOptions opts = OpenCliBrowserGetHtmlOptions.builder()
            .selector("body")
            .as("json")
            .max(10000)
            .depth(3)
            .childrenMax(50)
            .textMax(200)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--selector"));
        assertTrue(target.contains("--as"));
        assertTrue(target.contains("json"));
        assertTrue(target.contains("--max"));
        assertTrue(target.contains("10000"));
        assertTrue(target.contains("--depth"));
        assertTrue(target.contains("3"));
        assertTrue(target.contains("--children-max"));
        assertTrue(target.contains("50"));
        assertTrue(target.contains("--text-max"));
        assertTrue(target.contains("200"));
    }
}
