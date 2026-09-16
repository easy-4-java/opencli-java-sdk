package io.github.easy4j.opencli.adapter.browser.yuanbao;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.yuanbao.YuanbaoOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class YuanbaoOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final YuanbaoOpenCliClient client = new YuanbaoOpenCliClient(exec);

    @Test
    void shouldCallAskWithPositionalPrompt() {
        client.ask("总结这段文字", YuanbaoAskOptions.builder().think(true).build(), null);
        List<String> argv = exec.lastInvocation();
        assertEquals("yuanbao", argv.get(0));
        assertEquals("ask", argv.get(1));
        assertEquals("总结这段文字", argv.get(2));
        assertTrue(argv.contains("--think"));
    }

    @Test
    void shouldCallLifecycleCommands() {
        client.send("hi", true, null);
        List<String> send = exec.lastInvocation();
        assertEquals("send", send.get(1));
        assertTrue(send.contains("--new"));

        client.detail("conv-9", null);
        assertEquals("conv-9", exec.lastInvocation().get(2));

        client.history(3, null);
        assertTrue(exec.lastInvocation().contains("--limit"));

        client.read(null);
        assertEquals("read", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallSimpleCommands() {
        client.login(30, null);
        assertEquals("login", exec.lastInvocation().get(1));
        client.newChat(null);
        assertEquals("new", exec.lastInvocation().get(1));
        client.status(null);
        assertEquals("status", exec.lastInvocation().get(1));
        client.whoami(null);
        assertEquals("whoami", exec.lastInvocation().get(1));
    }
}
