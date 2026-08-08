package io.github.easy4j.opencli.adapter.browser.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BrowserLlmOptionsTest {

    @Test
    void shouldBuildWithDefaults() {
        BrowserLlmOptions opts = BrowserLlmOptions.builder().build();
        assertNull(opts.getTimeoutSeconds());
        assertNull(opts.getModel());
        assertNull(opts.getThinking());
        assertNull(opts.getStartNewChat());
        assertNull(opts.getSiteSession());
        assertNull(opts.getJsonOutput());
    }

    @Test
    void shouldAppendAllOptions() {
        BrowserLlmOptions opts = BrowserLlmOptions.builder()
            .timeoutSeconds(30)
            .model("gemini-pro")
            .thinking("high")
            .startNewChat(true)
            .siteSession("ephemeral")
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("30"));
        assertTrue(target.contains("--model"));
        assertTrue(target.contains("gemini-pro"));
        assertTrue(target.contains("--thinking"));
        assertTrue(target.contains("high"));
        assertTrue(target.contains("--new"));
        assertTrue(target.contains("--site-session"));
        assertTrue(target.contains("ephemeral"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }

    @Test
    void shouldNotAppendNullOptions() {
        BrowserLlmOptions opts = BrowserLlmOptions.builder().build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.isEmpty());
    }

    @Test
    void shouldNotAppendEmptyModel() {
        BrowserLlmOptions opts = BrowserLlmOptions.builder().model("").build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertFalse(target.contains("--model"));
    }

    @Test
    void shouldNotAppendFalseStartNewChat() {
        BrowserLlmOptions opts = BrowserLlmOptions.builder().startNewChat(false).build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertFalse(target.contains("--new"));
    }
}
