package io.github.easy4j.opencli;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliClientTest {

    @Test
    void shouldConstructWithDefaultProperties() {
        OpenCliClient client = new OpenCliClient();
        assertNotNull(client.getProperties());
        assertNotNull(client.getExecutor());
        assertEquals("opencli", client.getProperties().getExecutable());
    }

    @Test
    void shouldRejectNullProperties() {
        assertThrows(NullPointerException.class, () -> new OpenCliClient(null));
    }

    @Test
    void shouldRejectNullPropertiesWithExecutor() {
        OpenCliProperties props = new OpenCliProperties();
        io.github.easy4j.opencli.core.OpenCliExecutor executor = new io.github.easy4j.opencli.core.OpenCliExecutor(props);
        assertThrows(NullPointerException.class, () -> new OpenCliClient(null, executor));
    }

    @Test
    void shouldRejectNullExecutor() {
        OpenCliProperties props = new OpenCliProperties();
        assertThrows(NullPointerException.class, () -> new OpenCliClient(props, null));
    }

    @Test
    void shouldCreateAdapterChannel() {
        OpenCliClient client = new OpenCliClient();
        io.github.easy4j.opencli.core.OpenCliAdapterChannel ch = client.adapter("twitter");
        assertNotNull(ch);
        assertEquals("twitter", ch.getAdapterId());
    }

    @Test
    void shouldCreateAllTypedClients() {
        OpenCliClient client = new OpenCliClient();
        assertNotNull(client.codex());
        assertNotNull(client.cursor());
        assertNotNull(client.gemini());
        assertNotNull(client.claude());
        assertNotNull(client.chatgpt());
        assertNotNull(client.jimeng());
        assertNotNull(client.deepseek());
        assertNotNull(client.arxiv());
        assertNotNull(client.npm());
        assertNotNull(client.pypi());
        assertNotNull(client.binance());
        assertNotNull(client.wikipedia());
    }

    @Test
    void shouldCreateFacadeClients() {
        OpenCliClient client = new OpenCliClient();
        assertNotNull(client.publicApis());
        assertNotNull(client.browsers());
        assertNotNull(client.desktops());
        assertNotNull(client.meta());
        assertNotNull(client.browser());
    }

    @Test
    void shouldStoreSharedExecutor() {
        OpenCliProperties props = new OpenCliProperties();
        io.github.easy4j.opencli.core.OpenCliExecutor executor = new io.github.easy4j.opencli.core.OpenCliExecutor(props);
        OpenCliClient client = new OpenCliClient(props, executor);
        assertSame(executor, client.getExecutor());
        assertSame(props, client.getProperties());
    }
}
