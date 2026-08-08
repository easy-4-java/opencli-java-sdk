package io.github.easy4j.opencli.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserDragLocatorTest {

    @Test
    void shouldAppendFromAndToLocators() {
        OpenCliBrowserDragLocator loc = OpenCliBrowserDragLocator.builder()
            .fromRole("button")
            .fromName("source")
            .toRole("dropzone")
            .toName("target")
            .build();
        List<String> target = new ArrayList<>();
        loc.appendTo(target);
        assertTrue(target.contains("--from-role"));
        assertTrue(target.contains("button"));
        assertTrue(target.contains("--from-name"));
        assertTrue(target.contains("source"));
        assertTrue(target.contains("--to-role"));
        assertTrue(target.contains("dropzone"));
        assertTrue(target.contains("--to-name"));
        assertTrue(target.contains("target"));
    }
}
