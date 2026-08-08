package io.github.easy4j.opencli.adapter.browser.jimeng;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.adapter.browser.jimeng.JimengOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class JimengOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final JimengOpenCliClient client = new JimengOpenCliClient(exec);

    @Test void shouldCallLogin() { assertNotNull(client.login(30, null)); }
    @Test void shouldCallWhoami() { assertNotNull(client.whoami(null)); }
    @Test void shouldCallGenerate() { assertNotNull(client.generate("prompt", null, null)); }
    @Test void shouldCallGenerateWithOptions() {
        JimengGenerateOptions opts = JimengGenerateOptions.builder().model("m1").build();
        assertNotNull(client.generate("prompt", opts, null));
    }
    @Test void shouldCallGenerateImage2Image() { assertNotNull(client.generateImage2Image("p", "img1,img2", null, null)); }
    @Test void shouldCallGenerateVideo() { assertNotNull(client.generateVideo("p", null, null)); }
    @Test void shouldCallGenerateImage2Video() { assertNotNull(client.generateImage2Video("p", "/img.png", null, null)); }
    @Test void shouldCallGenerateAudio() { assertNotNull(client.generateAudio("p", null, null)); }
    @Test void shouldCallGenerateDigitalHuman() { assertNotNull(client.generateDigitalHuman("p", null, null)); }
    @Test void shouldCallGenerateActionCopy() { assertNotNull(client.generateActionCopy("/ref.png", null, null)); }
    @Test void shouldCallHistory() { assertNotNull(client.history(10, "image", "ws1", null)); }
    @Test void shouldCallNewWorkspace() { assertNotNull(client.newWorkspace("image", null)); }
    @Test void shouldCallWorkspaces() { assertNotNull(client.workspaces(null)); }
    @Test void shouldCallUserCredit() { assertNotNull(client.userCredit(null)); }
    @Test void shouldCallUserAssets() { assertNotNull(client.userAssets("tab1", 10, null)); }
    @Test void shouldCallUserSubscription() { assertNotNull(client.userSubscription(null)); }
}
