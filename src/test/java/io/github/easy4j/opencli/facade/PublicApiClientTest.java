package io.github.easy4j.opencli.facade;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliClient;
import org.junit.jupiter.api.Test;

class PublicApiClientTest {

    @Test
    void shouldHoldOpenCliClient() {
        OpenCliClient client = new OpenCliClient();
        PublicApiClient pa = new PublicApiClient(client);
        assertSame(client, pa.getOpenCli());
    }

    @Test
    void shouldDelegateToOpenCliClient() {
        OpenCliClient client = new OpenCliClient();
        PublicApiClient pa = new PublicApiClient(client);
        assertNotNull(pa.channel("npm"));
        assertNotNull(pa.arxiv());
        assertNotNull(pa.npm());
        assertNotNull(pa.pypi());
        assertNotNull(pa.binance());
        assertNotNull(pa.wikipedia());
    }
}
