package io.github.easy4j.opencli;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.easy4j.opencli.facade.BrowserClient;
import io.github.easy4j.opencli.facade.DesktopClient;
import io.github.easy4j.opencli.facade.PublicApiClient;
import org.junit.jupiter.api.Test;

class OpenCliClientTest {

    @Test
    void exposesAllMainEntryPoints() {
        OpenCliProperties properties = new OpenCliProperties();
        OpenCliClient client = new OpenCliClient(properties);
        assertSame(properties, client.getProperties());
        assertNotNull(client.getExecutor());
        assertNotNull(client.adapter("demo"));
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
        assertNotNull(client.publicApis());
        assertNotNull(client.browsers());
        assertNotNull(client.desktops());
        assertNotNull(client.meta());
        assertNotNull(client.browser());
    }

    @Test
    void facadeAccessorsExposeTypedAndGenericClients() {
        OpenCliClient client = new OpenCliClient();
        PublicApiClient publicApi = client.publicApis();
        assertNotNull(publicApi.arxiv());
        assertNotNull(publicApi.npm());
        assertNotNull(publicApi.pypi());
        assertNotNull(publicApi.binance());
        assertNotNull(publicApi.wikipedia());
        assertNotNull(publicApi.channel("demo"));

        BrowserClient browser = client.browsers();
        assertNotNull(browser.gemini());
        assertNotNull(browser.claude());
        assertNotNull(browser.chatgpt());
        assertNotNull(browser.jimeng());
        assertNotNull(browser.deepseek());
        assertNotNull(browser.channel("demo"));

        DesktopClient desktop = client.desktops();
        assertNotNull(desktop.codex());
        assertNotNull(desktop.cursor());
        assertNotNull(desktop.antigravity());
        assertNotNull(desktop.chatgptApp());
        assertNotNull(desktop.chatwise());
        assertNotNull(desktop.discordApp());
        assertNotNull(desktop.doubaoApp());
        assertNotNull(desktop.qoder());
        assertNotNull(desktop.traeCn());
        assertNotNull(desktop.traeSolo());
    }
}
