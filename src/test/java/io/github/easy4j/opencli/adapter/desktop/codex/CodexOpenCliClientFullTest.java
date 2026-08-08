package io.github.easy4j.opencli.adapter.desktop.codex;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.desktop.support.DesktopThreadSelection;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import org.junit.jupiter.api.Test;

class CodexOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final CodexOpenCliClient client = new CodexOpenCliClient(exec);

    @Test void shouldCallArchive() { assertNotNull(client.archive(true, null, null)); }
    @Test void shouldCallPin() { assertNotNull(client.pin(null, null)); }
    @Test void shouldCallRename() { assertNotNull(client.rename("new-title", null, null)); }
    @Test void shouldCallUnpin() { assertNotNull(client.unpin(null, null)); }
    @Test void shouldCallStatus() { assertNotNull(client.status()); }
    @Test void shouldCallDump() { assertNotNull(client.dump()); }
    @Test void shouldCallScreenshotNoArgs() { assertNotNull(client.screenshot()); }
    @Test void shouldCallScreenshotWithOutput() { assertNotNull(client.screenshot("/out")); }
    @Test void shouldCallNewSessionNoArgs() { assertNotNull(client.newSession()); }
    @Test void shouldCallNewSessionWithArgs() { assertNotNull(client.newSession(null)); }
    @Test void shouldCallSend() { assertNotNull(client.send("msg", null, null)); }
    @Test void shouldCallAsk() { assertNotNull(client.ask("msg", null, null)); }
    @Test void shouldCallReadNoArgs() { assertNotNull(client.read()); }
    @Test void shouldCallReadWithSelection() { assertNotNull(client.read(null, null)); }
    @Test void shouldCallProjectsNoArgs() { assertNotNull(client.projects()); }
    @Test void shouldCallProjectsWithArgs() { assertNotNull(client.projects("proj", 10, null)); }
    @Test void shouldCallHistoryNoArgs() { assertNotNull(client.history()); }
    @Test void shouldCallHistoryWithArgs() { assertNotNull(client.history("proj", 10, null)); }
    @Test void shouldCallExtractDiff() { assertNotNull(client.extractDiff()); }
    @Test void shouldCallModelNoArgs() { assertNotNull(client.model()); }
    @Test void shouldCallModelWithName() { assertNotNull(client.model("claude-3")); }
    @Test void shouldCallModelList() { assertNotNull(client.modelList()); }
    @Test void shouldCallExportConversationNoArgs() { assertNotNull(client.exportConversation()); }
    @Test void shouldCallExportConversationWithOutput() { assertNotNull(client.exportConversation("/out")); }
}
