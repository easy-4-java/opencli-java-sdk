package io.github.easy4j.opencli.adapter.desktop.cursor;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import org.junit.jupiter.api.Test;

class CursorOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final CursorOpenCliClient client = new CursorOpenCliClient(exec);

    @Test void shouldCallStatus() { assertNotNull(client.status()); }
    @Test void shouldCallDump() { assertNotNull(client.dump()); }
    @Test void shouldCallScreenshotNoArgs() { assertNotNull(client.screenshot()); }
    @Test void shouldCallScreenshotWithOutput() { assertNotNull(client.screenshot("/out")); }
    @Test void shouldCallNewTabNoArgs() { assertNotNull(client.newTab()); }
    @Test void shouldCallNewTabWithArgs() { assertNotNull(client.newTab(null)); }
    @Test void shouldCallSend() { assertNotNull(client.send("msg", null, null)); }
    @Test void shouldCallAsk() { assertNotNull(client.ask("msg", null, null)); }
    @Test void shouldCallReadNoArgs() { assertNotNull(client.read()); }
    @Test void shouldCallReadWithSelection() { assertNotNull(client.read(null, null)); }
    @Test void shouldCallComposer() { assertNotNull(client.composer("prompt", null)); }
    @Test void shouldCallModelNoArgs() { assertNotNull(client.model()); }
    @Test void shouldCallModelWithName() { assertNotNull(client.model("claude-3")); }
    @Test void shouldCallExtractCode() { assertNotNull(client.extractCode()); }
    @Test void shouldCallHistory() { assertNotNull(client.history()); }
    @Test void shouldCallExportConversationNoArgs() { assertNotNull(client.exportConversation()); }
    @Test void shouldCallExportConversationWithOutput() { assertNotNull(client.exportConversation("/out")); }
}
