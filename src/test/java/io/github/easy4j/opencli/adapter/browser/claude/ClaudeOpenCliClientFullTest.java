package io.github.easy4j.opencli.adapter.browser.claude;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.claude.ClaudeOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class ClaudeOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final ClaudeOpenCliClient client = new ClaudeOpenCliClient(exec);

    @Test
    void shouldCallAsk() {
        OpenCliResult r = client.ask("hello", null, null);
        assertNotNull(r);
        assertEquals("ask", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallSendWithPromptAndMore() {
        OpenCliResult r = client.send("msg", null);
        assertNotNull(r);
        assertEquals("send", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallSendWithNullOptions() {
        OpenCliResult r = client.send("msg", null, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallLogin() {
        OpenCliResult r = client.login(30, null);
        assertNotNull(r);
        assertEquals("login", exec.lastInvocation().get(1));
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
    void shouldCallHistoryWithMore() {
        OpenCliResult r = client.history(null);
        assertNotNull(r);
    }

    @Test
    void shouldCallHistoryWithLimit() {
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
        ClaudeAskOptions opts = ClaudeAskOptions.builder().jsonOutput(true).build();
        var r = client.askTyped("hello", opts, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallAskTypedWithToBuilderJson() {
        ClaudeAskOptions opts = ClaudeAskOptions.builder().timeoutSeconds(30).build();
        var r = client.askTyped("hello", opts, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallHistoryTyped() {
        var r = client.historyTyped(10, null);
        assertNotNull(r);
    }
}
