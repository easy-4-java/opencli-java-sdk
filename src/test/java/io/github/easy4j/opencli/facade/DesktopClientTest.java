package io.github.easy4j.opencli.facade;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliClient;
import org.junit.jupiter.api.Test;

class DesktopClientTest {

    @Test
    void shouldHoldOpenCliClient() {
        OpenCliClient client = new OpenCliClient();
        DesktopClient dc = new DesktopClient(client);
        assertSame(client, dc.getOpenCli());
    }

    @Test
    void shouldDelegateToOpenCliClient() {
        OpenCliClient client = new OpenCliClient();
        DesktopClient dc = new DesktopClient(client);
        assertNotNull(dc.codex());
        assertNotNull(dc.cursor());
        assertNotNull(dc.antigravity());
        assertNotNull(dc.chatgptApp());
        assertNotNull(dc.chatwise());
        assertNotNull(dc.discordApp());
        assertNotNull(dc.doubaoApp());
        assertNotNull(dc.qoder());
        assertNotNull(dc.traeCn());
        assertNotNull(dc.traeSolo());
    }

    @Test
    void deprecatedDiscordShouldDelegateToDiscordApp() {
        OpenCliClient client = new OpenCliClient();
        DesktopClient dc = new DesktopClient(client);
        assertNotNull(dc.discord());
    }
}
