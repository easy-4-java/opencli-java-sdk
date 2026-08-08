package io.github.easy4j.opencli.adapter.browser.deepseek;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.deepseek.DeepseekOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class DeepseekOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final DeepseekOpenCliClient client = new DeepseekOpenCliClient(exec);

    @Test
    void shouldCallAsk() {
        OpenCliResult r = client.ask("hello", null, null);
        assertNotNull(r);
        assertEquals("ask", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallSend() {
        OpenCliResult r = client.send("conv-1", "msg", null, null);
        assertNotNull(r);
        assertEquals("send", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallLogin() {
        OpenCliResult r = client.login(30, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallWhoami() {
        OpenCliResult r = client.whoami(null);
        assertNotNull(r);
    }

    @Test
    void shouldCallNewChat() {
        OpenCliResult r = client.newChat(null);
        assertNotNull(r);
    }

    @Test
    void shouldCallStatus() {
        OpenCliResult r = client.status(null);
        assertNotNull(r);
    }

    @Test
    void shouldCallRead() {
        OpenCliResult r = client.read(null);
        assertNotNull(r);
    }

    @Test
    void shouldCallHistory() {
        OpenCliResult r = client.history(10, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallDetail() {
        OpenCliResult r = client.detail("conv-id", null);
        assertNotNull(r);
    }

    @Test
    void shouldCallAskTyped() {
        var r = client.askTyped("hello", null, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallAskTypedWithExistingJson() {
        DeepseekAskOptions opts = DeepseekAskOptions.builder().jsonOutput(true).build();
        var r = client.askTyped("hello", opts, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallAskTypedWithToBuilder() {
        DeepseekAskOptions opts = DeepseekAskOptions.builder().timeoutSeconds(30).build();
        var r = client.askTyped("hello", opts, null);
        assertNotNull(r);
    }
}
