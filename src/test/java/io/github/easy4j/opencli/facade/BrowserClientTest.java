package io.github.easy4j.opencli.facade;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliClient;
import org.junit.jupiter.api.Test;

class BrowserClientTest {

    @Test
    void shouldHoldOpenCliClient() {
        OpenCliClient client = new OpenCliClient();
        BrowserClient bc = new BrowserClient(client);
        assertSame(client, bc.getOpenCli());
    }

    @Test
    void shouldDelegateToOpenCliClient() {
        OpenCliClient client = new OpenCliClient();
        BrowserClient bc = new BrowserClient(client);
        assertNotNull(bc.channel("chatgpt"));
        assertNotNull(bc.gemini());
        assertNotNull(bc.claude());
        assertNotNull(bc.chatgpt());
        assertNotNull(bc.jimeng());
        assertNotNull(bc.deepseek());
    }
}
