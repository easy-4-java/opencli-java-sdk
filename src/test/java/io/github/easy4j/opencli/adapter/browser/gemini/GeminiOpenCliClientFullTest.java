package io.github.easy4j.opencli.adapter.browser.gemini;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.gemini.GeminiOpenCliClient.*;
import io.github.easy4j.opencli.adapter.browser.support.BrowserLlmOptions;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class GeminiOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final GeminiOpenCliClient client = new GeminiOpenCliClient(exec);

    @Test void shouldCallNewChatWithArgs() { assertNotNull(client.newChat(null)); }
    @Test void shouldCallNewChatNoArgs() { assertNotNull(client.newChat()); }
    @Test void shouldCallAsk() { assertNotNull(client.ask("hi", null, null)); }
    @Test void shouldCallAskWithOptions() {
        BrowserLlmOptions opts = BrowserLlmOptions.builder().timeoutSeconds(30).build();
        assertNotNull(client.ask("hi", opts, null));
    }
    @Test void shouldCallImage() { assertNotNull(client.image("cat", null, null)); }
    @Test void shouldCallImageWithArgs() {
        GeminiImageArgs args = GeminiImageArgs.builder().aspectRatio("1:1").build();
        assertNotNull(client.image("cat", args, null));
    }
    @Test void shouldCallDeepResearch() { assertNotNull(client.deepResearch("topic", (GeminiDeepResearchOptions) null, null)); }
    @Test void shouldCallDeepResearchWithBrowserOpts() {
        BrowserLlmOptions opts = BrowserLlmOptions.builder().timeoutSeconds(60).build();
        assertNotNull(client.deepResearch("topic", opts, null));
    }
    @Test void shouldCallDeepResearchWithNullBrowserOpts() {
        assertNotNull(client.deepResearch("topic", (BrowserLlmOptions) null, null));
    }
    @Test void shouldCallDetail() { assertNotNull(client.detail("id", null)); }
    @Test void shouldCallHistoryWithLimit() { assertNotNull(client.history(10, null)); }
    @Test void shouldCallHistoryNoLimit() { assertNotNull(client.history(null)); }
    @Test void shouldCallLogin() { assertNotNull(client.login(30, null)); }
    @Test void shouldCallModels() { assertNotNull(client.models(null)); }
    @Test void shouldCallRead() { assertNotNull(client.read(null)); }
    @Test void shouldCallStatus() { assertNotNull(client.status(null)); }
    @Test void shouldCallWhoami() { assertNotNull(client.whoami(null)); }
    @Test void shouldCallDeepResearchResult() { assertNotNull(client.deepResearchResult("q", "m", 30, null)); }
    @Test void shouldCallDeepResearchResultShort() { assertNotNull(client.deepResearchResult("q", null)); }
}
