package io.github.easy4j.opencli.adapter.browser.jimeng;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.jimeng.JimengOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class JimengOpenCliClientTest {

    @Test
    void shouldBuildGenerateOptions() {
        JimengGenerateOptions opts = JimengGenerateOptions.builder()
            .model("jimeng-2.1")
            .waitSeconds(60)
            .timeoutSeconds(120)
            .workspace("ws-1")
            .tone("female")
            .toneFile("/tone.wav")
            .cloneFile("/clone.wav")
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--model"));
        assertTrue(target.contains("jimeng-2.1"));
        assertTrue(target.contains("--wait=60"));
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("120"));
        assertTrue(target.contains("--workspace=ws-1"));
        assertTrue(target.contains("--tone"));
        assertTrue(target.contains("female"));
        assertTrue(target.contains("--tone-file"));
        assertTrue(target.contains("/tone.wav"));
        assertTrue(target.contains("--clone-file"));
        assertTrue(target.contains("/clone.wav"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }
}
