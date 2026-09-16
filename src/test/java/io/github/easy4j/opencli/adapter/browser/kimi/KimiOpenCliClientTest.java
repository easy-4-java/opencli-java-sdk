package io.github.easy4j.opencli.adapter.browser.kimi;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.kimi.KimiOpenCliClient.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class KimiOpenCliClientTest {

    @Test
    void shouldBuildAskOptions() {
        KimiAskOptions opts = KimiAskOptions.builder()
            .timeoutSeconds(120)
            .jsonOutput(true)
            .build();
        List<String> target = new ArrayList<>();
        opts.appendTo(target);
        assertTrue(target.contains("--timeout"));
        assertTrue(target.contains("120"));
        assertTrue(target.contains("-f"));
        assertTrue(target.contains("json"));
    }

    @Test
    void shouldBuildReadAndModelOptions() {
        KimiReadOptions read = KimiReadOptions.builder().conv("c1").limit(20).build();
        List<String> target = new ArrayList<>();
        read.appendTo(target);
        assertTrue(target.contains("--conv"));
        assertTrue(target.contains("c1"));
        assertTrue(target.contains("--limit"));

        KimiModelOptions model = KimiModelOptions.builder().list(true).build();
        List<String> target2 = new ArrayList<>();
        model.appendTo(target2);
        assertTrue(target2.contains("--list"));
        assertFalse(target2.contains("--set"));

        KimiModelOptions set = KimiModelOptions.builder().set("K2.6").build();
        List<String> target3 = new ArrayList<>();
        set.appendTo(target3);
        assertTrue(target3.contains("--set"));
        assertTrue(target3.contains("K2.6"));
    }

    @Test
    void shouldBuildStorageAndTemplateOptions() {
        KimiStorageKeysOptions keys = KimiStorageKeysOptions.builder()
            .storage("local")
            .filter("kimi")
            .limit(50)
            .build();
        List<String> target = new ArrayList<>();
        keys.appendTo(target);
        assertTrue(target.contains("--storage"));
        assertTrue(target.contains("--filter"));
        assertTrue(target.contains("--limit"));

        KimiTemplatesOptions tpl = KimiTemplatesOptions.builder().mode("ppt").limit(10).build();
        List<String> target2 = new ArrayList<>();
        tpl.appendTo(target2);
        assertTrue(target2.contains("--mode"));
        assertTrue(target2.contains("ppt"));
    }
}
