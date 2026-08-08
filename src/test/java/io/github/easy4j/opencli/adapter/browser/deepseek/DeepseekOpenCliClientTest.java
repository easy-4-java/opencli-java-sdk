package io.github.easy4j.opencli.adapter.browser.deepseek;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.deepseek.DeepseekOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DeepseekOpenCliClientTest {

    @Test
    void shouldBuildAskOptions() {
        DeepseekAskOptions opts = DeepseekAskOptions.builder()
            .timeoutSeconds(30)
            .newConversation(true)
            .model("expert")
            .deepThink(true)
            .webSearch(true)
            .attachmentPath("/file.txt")
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--new"));
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("--model"));
        assertTrue(target.contains("expert"));
        assertTrue(target.contains("--think"));
        assertTrue(target.contains("--search"));
        assertTrue(target.contains("--file"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }

    @Test
    void shouldSupportToBuilder() {
        DeepseekAskOptions opts = DeepseekAskOptions.builder().model("instant").build();
        DeepseekAskOptions copy = opts.toBuilder().deepThink(true).build();
        assertEquals("instant", copy.getModel());
        assertTrue(copy.getDeepThink());
    }
}
