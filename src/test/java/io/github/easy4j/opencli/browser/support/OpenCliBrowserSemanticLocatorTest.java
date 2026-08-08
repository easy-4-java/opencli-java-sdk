package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserSemanticLocatorTest {

    @Test
    void shouldAppendAllLocatorFields() {
        OpenCliBrowserSemanticLocator loc = OpenCliBrowserSemanticLocator.builder()
            .role("button")
            .name("submit")
            .label("Submit")
            .text("Click me")
            .testId("btn-submit")
            .build();
        List<String> target = new ArrayList<>();
        loc.appendTo(target);
        assertTrue(target.contains("--role"));
        assertTrue(target.contains("button"));
        assertTrue(target.contains("--name"));
        assertTrue(target.contains("submit"));
        assertTrue(target.contains("--label"));
        assertTrue(target.contains("Submit"));
        assertTrue(target.contains("--text"));
        assertTrue(target.contains("Click me"));
        assertTrue(target.contains("--testid"));
        assertTrue(target.contains("btn-submit"));
    }

    @Test
    void shouldNotAppendNullFields() {
        OpenCliBrowserSemanticLocator loc = OpenCliBrowserSemanticLocator.builder().build();
        List<String> target = new ArrayList<>();
        loc.appendTo(target);
        assertTrue(target.isEmpty());
    }
}
