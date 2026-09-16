package io.github.easy4j.opencli.adapter.browser.kimi;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.kimi.KimiOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class KimiOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final KimiOpenCliClient client = new KimiOpenCliClient(exec);

    @Test
    void shouldCallAskWithPositionalText() {
        client.ask("调研 codegraph", KimiAskOptions.builder().timeoutSeconds(90).build(), null);
        List<String> argv = exec.lastInvocation();
        assertEquals("kimi", argv.get(0));
        assertEquals("ask", argv.get(1));
        assertEquals("调研 codegraph", argv.get(2));
        assertTrue(argv.contains("--timeout"));
    }

    @Test
    void shouldCallConversationCommands() {
        client.send("继续", null);
        assertEquals("send", exec.lastInvocation().get(1));
        assertEquals("继续", exec.lastInvocation().get(2));

        client.read("c-1", 10, null);
        List<String> read = exec.lastInvocation();
        assertEquals("read", read.get(1));
        assertTrue(read.contains("--conv"));

        client.detail("c-1", 5, null);
        assertEquals("c-1", exec.lastInvocation().get(2));

        client.history(8, null);
        assertTrue(exec.lastInvocation().contains("--limit"));
    }

    @Test
    void shouldCallUniqueKimiCommands() {
        client.mode("deep-research", null);
        List<String> mode = exec.lastInvocation();
        assertEquals("mode", mode.get(1));
        assertEquals("deep-research", mode.get(2));

        client.modelList(null);
        assertTrue(exec.lastInvocation().contains("--list"));
        client.modelSet("K2.6", null);
        assertTrue(exec.lastInvocation().contains("--set"));

        client.react("like", "c-2", null);
        List<String> react = exec.lastInvocation();
        assertEquals("react", react.get(1));
        assertEquals("like", react.get(2));
        assertTrue(react.contains("--conv"));

        client.regenerate("c-2", null);
        assertEquals("regenerate", exec.lastInvocation().get(1));

        client.historyRename("chat-1", "新标题", true, null);
        List<String> rename = exec.lastInvocation();
        assertEquals("chat-1", rename.get(2));
        assertEquals("新标题", rename.get(3));
        assertTrue(rename.contains("--yes"));
    }

    @Test
    void shouldCallUtilityCommands() {
        client.storageGet("theme", "local", 1024, null);
        List<String> get = exec.lastInvocation();
        assertEquals("storage-get", get.get(1));
        assertEquals("theme", get.get(2));
        assertTrue(get.contains("--max-bytes"));

        client.storageKeys(KimiStorageKeysOptions.builder().filter("kimi").build(), null);
        assertTrue(exec.lastInvocation().contains("--filter"));

        client.templates(KimiTemplatesOptions.builder().mode("docs").limit(5).build(), null);
        assertTrue(exec.lastInvocation().contains("--mode"));

        client.signOut(true, null);
        assertTrue(exec.lastInvocation().contains("--yes"));
        client.sidebarToggle(null);
        assertEquals("sidebar-toggle", exec.lastInvocation().get(1));
        client.usage(null);
        assertEquals("usage", exec.lastInvocation().get(1));
    }
}
