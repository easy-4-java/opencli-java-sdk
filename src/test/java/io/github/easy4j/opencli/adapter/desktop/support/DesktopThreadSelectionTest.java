package io.github.easy4j.opencli.adapter.desktop.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DesktopThreadSelectionTest {

    @Test
    void shouldAppendAllFields() {
        DesktopThreadSelection sel = DesktopThreadSelection.builder()
            .project("my-project")
            .conversation("test-conv")
            .conversationIndex(3)
            .threadId("local:abc123")
            .timeoutSeconds(60)
            .build();
        List<String> target = new ArrayList<>();
        sel.appendTo(target);
        assertTrue(target.contains("--project"));
        assertTrue(target.contains("my-project"));
        assertTrue(target.contains("--conversation"));
        assertTrue(target.contains("test-conv"));
        assertTrue(target.contains("--index"));
        assertTrue(target.contains("3"));
        assertTrue(target.contains("--thread-id"));
        assertTrue(target.contains("local:abc123"));
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("60"));
    }

    @Test
    void shouldNotAppendNullFields() {
        DesktopThreadSelection sel = DesktopThreadSelection.builder().build();
        List<String> target = new ArrayList<>();
        sel.appendTo(target);
        assertTrue(target.isEmpty());
    }
}
