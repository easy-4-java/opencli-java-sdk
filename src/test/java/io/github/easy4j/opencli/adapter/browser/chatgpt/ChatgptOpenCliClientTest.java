package io.github.easy4j.opencli.adapter.browser.chatgpt;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.chatgpt.ChatgptOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ChatgptOpenCliClientTest {

    @Test
    void shouldBuildCommonOptions() {
        ChatgptCommonOptions opts = ChatgptCommonOptions.builder()
            .timeoutSeconds(30)
            .historyLimit(10)
            .stableSeconds(5)
            .readAsMarkdown(true)
            .newConversation(true)
            .conversation("conv-1")
            .project("proj-1")
            .waitForResponse(true)
            .deepResearch(true)
            .webSearch(true)
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--new"));
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("30"));
        assertTrue(target.contains("--limit"));
        assertTrue(target.contains("10"));
        assertTrue(target.contains("--stable"));
        assertTrue(target.contains("5"));
        assertTrue(target.contains("--markdown"));
        assertTrue(target.contains("--conversation"));
        assertTrue(target.contains("conv-1"));
        assertTrue(target.contains("--project"));
        assertTrue(target.contains("proj-1"));
        assertTrue(target.contains("--wait"));
        assertTrue(target.contains("--deep-research"));
        assertTrue(target.contains("--web-search"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }

    @Test
    void shouldNotAppendNullCommonOptions() {
        ChatgptCommonOptions opts = ChatgptCommonOptions.builder().build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.isEmpty());
    }

    @Test
    void shouldBuildImageOptions() {
        ChatgptImageOptions opts = ChatgptImageOptions.builder()
            .referenceImagePath("/path/to/image.png")
            .outputDir("/output")
            .skipDownload(true)
            .timeoutSeconds(60)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--image"));
        assertTrue(target.contains("/path/to/image.png"));
        assertTrue(target.contains("--op"));
        assertTrue(target.contains("/output"));
        assertTrue(target.contains("--sd"));
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("60"));
    }

    @Test
    void shouldNotAppendNullImageOptions() {
        ChatgptImageOptions opts = ChatgptImageOptions.builder().build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.isEmpty());
    }
}
