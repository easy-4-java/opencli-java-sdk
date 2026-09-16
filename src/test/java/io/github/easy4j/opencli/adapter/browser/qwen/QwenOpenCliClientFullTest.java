package io.github.easy4j.opencli.adapter.browser.qwen;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.qwen.QwenOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class QwenOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final QwenOpenCliClient client = new QwenOpenCliClient(exec);

    @Test
    void shouldCallAskWithPositionalPrompt() {
        client.ask("写一首诗", QwenAskOptions.builder().think(true).build(), null);
        List<String> argv = exec.lastInvocation();
        assertEquals("qwen", argv.get(0));
        assertEquals("ask", argv.get(1));
        assertEquals("写一首诗", argv.get(2));
        assertTrue(argv.contains("--think"));
    }

    @Test
    void shouldCallAskTypedForcingJson() {
        client.askTyped("hi", null, null);
        List<String> argv = exec.lastInvocation();
        assertTrue(argv.contains("-f"));
        assertTrue(argv.contains("json"));
    }

    @Test
    void shouldCallSendReadDetailHistory() {
        client.send("你好", null, null);
        assertEquals("send", exec.lastInvocation().get(1));
        assertEquals("你好", exec.lastInvocation().get(2));

        client.read(true, null);
        List<String> read = exec.lastInvocation();
        assertEquals("read", read.get(1));
        assertTrue(read.contains("--markdown"));

        client.detail("conv-1", null, null);
        assertEquals("conv-1", exec.lastInvocation().get(2));

        client.history(5, null);
        assertTrue(exec.lastInvocation().contains("--limit"));
    }

    @Test
    void shouldCallImage() {
        client.image("一只猫", QwenImageOptions.builder().outputPath("./x").build(), null);
        List<String> argv = exec.lastInvocation();
        assertEquals("image", argv.get(1));
        assertEquals("一只猫", argv.get(2));
        assertTrue(argv.contains("--op"));
    }

    @Test
    void shouldCallSimpleCommands() {
        client.login(60, null);
        assertEquals("login", exec.lastInvocation().get(1));
        client.newChat(null);
        assertEquals("new", exec.lastInvocation().get(1));
        client.status(null);
        assertEquals("status", exec.lastInvocation().get(1));
        client.whoami(null);
        assertEquals("whoami", exec.lastInvocation().get(1));
    }
}
