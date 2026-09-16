package io.github.easy4j.opencli.adapter.browser.doubao;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.doubao.DoubaoOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DoubaoOpenCliClientTest {

    @Test
    void shouldBuildAskOptions() {
        DoubaoAskOptions opts = DoubaoAskOptions.builder()
            .timeoutSeconds(60)
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("60"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }
}
