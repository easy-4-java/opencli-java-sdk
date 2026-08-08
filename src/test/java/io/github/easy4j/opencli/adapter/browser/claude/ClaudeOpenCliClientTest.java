package io.github.easy4j.opencli.adapter.browser.claude;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.claude.ClaudeOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ClaudeOpenCliClientTest {

    @Test
    void shouldBuildAskOptions() {
        ClaudeAskOptions opts = ClaudeAskOptions.builder()
            .timeoutSeconds(30)
            .newConversation(true)
            .model("claude-3.5-sonnet")
            .adaptiveThinking(true)
            .attachmentPath("/file.txt")
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--new"));
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("30"));
        assertTrue(target.contains("--model"));
        assertTrue(target.contains("claude-3.5-sonnet"));
        assertTrue(target.contains("--think"));
        assertTrue(target.contains("--file"));
        assertTrue(target.contains("/file.txt"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }

    @Test
    void shouldSupportToBuilder() {
        ClaudeAskOptions opts = ClaudeAskOptions.builder().model("claude-3").build();
        ClaudeAskOptions copy = opts.toBuilder().timeoutSeconds(60).build();
        assertEquals("claude-3", copy.getModel());
        assertEquals(60, copy.getTimeoutSeconds());
    }
}
