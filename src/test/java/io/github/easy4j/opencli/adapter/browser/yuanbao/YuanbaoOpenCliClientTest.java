package io.github.easy4j.opencli.adapter.browser.yuanbao;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.yuanbao.YuanbaoOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class YuanbaoOpenCliClientTest {

    @Test
    void shouldBuildAskOptions() {
        YuanbaoAskOptions opts = YuanbaoAskOptions.builder()
            .timeoutSeconds(45)
            .search(true)
            .think(true)
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("45"));
        assertTrue(target.contains("--search"));
        assertTrue(target.contains("--think"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }
}
