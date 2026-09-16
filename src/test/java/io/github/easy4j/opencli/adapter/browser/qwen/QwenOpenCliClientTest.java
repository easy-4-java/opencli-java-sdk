package io.github.easy4j.opencli.adapter.browser.qwen;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.qwen.QwenOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class QwenOpenCliClientTest {

    @Test
    void shouldBuildAskOptions() {
        QwenAskOptions opts = QwenAskOptions.builder()
            .timeoutSeconds(30)
            .newConversation(true)
            .think(true)
            .research(true)
            .markdown(true)
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--new"));
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("30"));
        assertTrue(target.contains("--think"));
        assertTrue(target.contains("--research"));
        assertTrue(target.contains("--markdown"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }

    @Test
    void shouldBuildImageOptions() {
        QwenImageOptions opts = QwenImageOptions.builder()
            .outputPath("./out")
            .sdModel(true)
            .newConversation(true)
            .timeoutSeconds(120)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--op"));
        assertTrue(target.contains("./out"));
        assertTrue(target.contains("--sd"));
        assertTrue(target.contains("--new"));
        assertTrue(target.contains("--timeout"));
    }
}
