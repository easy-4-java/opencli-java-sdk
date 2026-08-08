package io.github.easy4j.opencli.browser;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import org.junit.jupiter.api.Test;

class OpenCliBrowserClientTest {

    private final OpenCliExecutor executor = new OpenCliExecutor(new OpenCliProperties());
    private final OpenCliBrowserClient client = new OpenCliBrowserClient(executor);

    @Test
    void shouldCreateSession() {
        OpenCliBrowserSession session = client.session("test-session");
        assertNotNull(session);
    }

    @Test
    void shouldCreateSessionWithWindowMode() {
        OpenCliBrowserSession session = client.session("test-session", "background");
        assertNotNull(session);
    }

    @Test
    void shouldRejectNullSessionName() {
        assertThrows(NullPointerException.class, () -> client.session(null));
    }

    @Test
    void shouldRejectBlankSessionName() {
        assertThrows(IllegalArgumentException.class, () -> client.session("   "));
    }

    @Test
    void shouldRejectInvalidWindowMode() {
        assertThrows(IllegalArgumentException.class, () -> client.session("test", "invalid"));
    }

    @Test
    void shouldAcceptForegroundWindowMode() {
        assertNotNull(client.session("test", "foreground"));
    }

    @Test
    void shouldAcceptBackgroundWindowMode() {
        assertNotNull(client.session("test", "background"));
    }

    @Test
    void shouldNormalizeWindowMode() {
        OpenCliBrowserSession session = client.session("test", "FOREGROUND");
        assertNotNull(session);
    }
}
