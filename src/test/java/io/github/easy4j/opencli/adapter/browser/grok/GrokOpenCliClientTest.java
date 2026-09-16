package io.github.easy4j.opencli.adapter.browser.grok;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.grok.GrokOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GrokOpenCliClientTest {

    @Test
    void shouldBuildAskOptions() {
        GrokAskOptions opts = GrokAskOptions.builder()
            .timeoutSeconds(90)
            .newConversation(true)
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("90"));
        assertTrue(target.contains("--new"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }

    @Test
    void shouldBuildImageAndExportOptions() {
        GrokImageOptions img = GrokImageOptions.builder()
            .count(2)
            .outputPath("./grok")
            .newConversation(true)
            .timeoutSeconds(120)
            .build();
        List<String> target = new ArrayList<>();
        img.appendTo(target);
        assertTrue(target.contains("--count"));
        assertTrue(target.contains("2"));
        assertTrue(target.contains("--out"));
        assertTrue(target.contains("./grok"));

        GrokExportAllOptions all = GrokExportAllOptions.builder()
            .limit(10)
            .offset(5)
            .manifestPath("./m.json")
            .maxScrolls(30)
            .pageScrolls(3)
            .pageTimeoutMs(5000)
            .delayMinMs(100)
            .delayMaxMs(200)
            .build();
        List<String> target2 = new ArrayList<>();
        all.appendTo(target2);
        assertTrue(target2.contains("--offset"));
        assertTrue(target2.contains("--manifestPath"));
        assertTrue(target2.contains("--pageTimeoutMs"));
        assertTrue(target2.contains("--delayMinMs"));
    }
}
