package io.github.easy4j.opencli.adapter.browser.gemini;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.gemini.GeminiOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GeminiOpenCliClientTest {

    @Test
    void shouldBuildImageArgs() {
        GeminiImageArgs args = GeminiImageArgs.builder()
            .aspectRatio("16:9")
            .style("realistic")
            .outputDir("/out")
            .skipDownload(true)
            .timeoutSeconds(60)
            .build();
        List<String> target = new ArrayList<>();
        args.appendTo(target);
        assertTrue(target.contains("--rt"));
        assertTrue(target.contains("16:9"));
        assertTrue(target.contains("--st"));
        assertTrue(target.contains("realistic"));
        assertTrue(target.contains("--op"));
        assertTrue(target.contains("/out"));
        assertTrue(target.contains("--sd"));
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("60"));
    }

    @Test
    void shouldBuildDeepResearchOptions() {
        GeminiDeepResearchOptions opts = GeminiDeepResearchOptions.builder()
            .timeoutSeconds(120)
            .model("gemini-pro")
            .thinking("high")
            .tool("search")
            .confirmLabel("confirm")
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("120"));
        assertTrue(target.contains("--model"));
        assertTrue(target.contains("gemini-pro"));
        assertTrue(target.contains("--thinking"));
        assertTrue(target.contains("high"));
        assertTrue(target.contains("--tool"));
        assertTrue(target.contains("search"));
        assertTrue(target.contains("--confirm"));
        assertTrue(target.contains("confirm"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }
}
