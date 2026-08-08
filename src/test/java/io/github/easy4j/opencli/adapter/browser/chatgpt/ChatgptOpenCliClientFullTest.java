package io.github.easy4j.opencli.adapter.browser.chatgpt;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.chatgpt.ChatgptOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.core.OpenCliTypedResult;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class ChatgptOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final ChatgptOpenCliClient client = new ChatgptOpenCliClient(exec);

    @Test
    void shouldCallAskWithOptions() {
        ChatgptCommonOptions opts = ChatgptCommonOptions.builder().timeoutSeconds(30).jsonOutput(true).build();
        OpenCliResult r = client.ask("hello", opts, null);
        assertNotNull(r);
        List<String> argv = exec.lastInvocation();
        assertEquals("chatgpt", argv.get(0));
        assertEquals("ask", argv.get(1));
        assertEquals("hello", argv.get(2));
    }

    @Test
    void shouldCallAskWithNullOptions() {
        OpenCliResult r = client.ask("hello", null, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallSend() {
        OpenCliResult r = client.send("msg", null, null);
        assertNotNull(r);
        assertEquals("send", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallRead() {
        OpenCliResult r = client.read(null, null);
        assertNotNull(r);
        assertEquals("read", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallHistory() {
        OpenCliResult r = client.history(null, null);
        assertNotNull(r);
        assertEquals("history", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallDetail() {
        OpenCliResult r = client.detail("id-1", null, null);
        assertNotNull(r);
        assertEquals("detail", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallNewChat() {
        OpenCliResult r = client.newChat(null);
        assertNotNull(r);
        assertEquals("new", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallStatus() {
        OpenCliResult r = client.status(null);
        assertNotNull(r);
        assertEquals("status", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallDeepResearchResult() {
        OpenCliResult r = client.deepResearchResult("id-1", true, 60, 10, null);
        assertNotNull(r);
        assertEquals("deep-research-result", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallLogin() {
        OpenCliResult r = client.login(30, null);
        assertNotNull(r);
        assertEquals("login", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallModel() {
        OpenCliResult r = client.model("gpt-4", "proj", null);
        assertNotNull(r);
        assertEquals("model", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallProjectFileAdd() {
        OpenCliResult r = client.projectFileAdd("file.txt", "proj-id", null);
        assertNotNull(r);
        assertEquals("project-file-add", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallProjectList() {
        OpenCliResult r = client.projectList(10, null);
        assertNotNull(r);
        assertEquals("project-list", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallWhoami() {
        OpenCliResult r = client.whoami(null);
        assertNotNull(r);
        assertEquals("whoami", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallImageWithOptions() {
        ChatgptImageOptions opts = ChatgptImageOptions.builder().outputDir("/out").build();
        OpenCliResult r = client.image("a cat", opts, null);
        assertNotNull(r);
        assertEquals("image", exec.lastInvocation().get(1));
    }

    @Test
    void shouldCallImageWithoutOptions() {
        OpenCliResult r = client.image("a cat", null);
        assertNotNull(r);
    }

    @Test
    void shouldCallAskTyped() {
        OpenCliTypedResult<JsonNode> r = client.askTyped("hello", null, null);
        assertNotNull(r);
        assertNotNull(r.getStructured());
    }

    @Test
    void shouldCallHistoryTyped() {
        OpenCliTypedResult<JsonNode> r = client.historyTyped(null, null);
        assertNotNull(r);
    }

    @Test
    void shouldCallAskTypedWithExistingJsonOption() {
        ChatgptCommonOptions opts = ChatgptCommonOptions.builder().jsonOutput(true).build();
        OpenCliTypedResult<JsonNode> r = client.askTyped("hello", opts, null);
        assertNotNull(r);
    }
}
