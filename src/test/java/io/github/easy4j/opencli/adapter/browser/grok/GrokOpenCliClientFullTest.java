package io.github.easy4j.opencli.adapter.browser.grok;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.grok.GrokOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class GrokOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final GrokOpenCliClient client = new GrokOpenCliClient(exec);

    @Test
    void shouldCallAskWithPositionalPrompt() {
        client.ask("解释量子纠缠", GrokAskOptions.builder().newConversation(true).build(), null);
        List<String> argv = exec.lastInvocation();
        assertEquals("grok", argv.get(0));
        assertEquals("ask", argv.get(1));
        assertEquals("解释量子纠缠", argv.get(2));
        assertTrue(argv.contains("--new"));
    }

    @Test
    void shouldCallConversationManagement() {
        client.delete("conv-1", true, null);
        List<String> del = exec.lastInvocation();
        assertEquals("delete", del.get(1));
        assertEquals("conv-1", del.get(2));
        assertTrue(del.contains("--yes"));

        client.pin("conv-1", null);
        assertEquals("pin", exec.lastInvocation().get(1));
        client.unpin("conv-1", null);
        assertEquals("unpin", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallExportCommands() {
        client.export(GrokExportOptions.builder().limit(5).maxScrolls(10).build(), null);
        List<String> export = exec.lastInvocation();
        assertEquals("export", export.get(1));
        assertTrue(export.contains("--maxScrolls"));

        client.exportAll(GrokExportAllOptions.builder().limit(5).build(), null);
        assertEquals("export-all", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallImage() {
        client.image("a cat", GrokImageOptions.builder().count(2).outputPath("./g").build(), null);
        List<String> argv = exec.lastInvocation();
        assertEquals("image", argv.get(1));
        assertEquals("a cat", argv.get(2));
        assertTrue(argv.contains("--count"));
        assertTrue(argv.contains("--out"));
    }

    @Test
    void shouldCallSimpleCommands() {
        client.read(null, null);
        assertEquals("read", exec.lastInvocation().get(1));
        client.detail("conv-3", true, null);
        assertTrue(exec.lastInvocation().contains("--markdown"));
        client.login(45, null);
        assertEquals("login", exec.lastInvocation().get(1));
        client.whoami(null);
        assertEquals("whoami", exec.lastInvocation().get(1));
    }
}
