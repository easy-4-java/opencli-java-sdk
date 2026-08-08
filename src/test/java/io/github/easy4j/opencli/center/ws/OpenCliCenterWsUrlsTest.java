package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliCenterWsUrlsTest {

    @Test
    void shouldConvertHttpToWs() {
        String url = OpenCliCenterWsUrls.toAgentWebSocketUrl(
            "http://192.168.1.1:8031", OpenCliCenterWebSocketPath.NODES_WS);
        assertEquals("ws://192.168.1.1:8031/api/v1/nodes/ws", url);
    }

    @Test
    void shouldConvertHttpsToWss() {
        String url = OpenCliCenterWsUrls.toAgentWebSocketUrl(
            "https://example.com", OpenCliCenterWebSocketPath.BROWSERS_AGENTS_WS);
        assertEquals("wss://example.com/api/v1/browsers/agents/ws", url);
    }

    @Test
    void shouldStripTrailingSlashes() {
        String url = OpenCliCenterWsUrls.toAgentWebSocketUrl(
            "http://host:8031///", OpenCliCenterWebSocketPath.NODES_WS);
        assertEquals("ws://host:8031/api/v1/nodes/ws", url);
    }

    @Test
    void shouldThrowForNullPath() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCenterWsUrls.toAgentWebSocketUrl("http://host", null));
    }

    @Test
    void shouldThrowForBlankBase() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCenterWsUrls.toAgentWebSocketUrl("", OpenCliCenterWebSocketPath.NODES_WS));
    }

    @Test
    void shouldThrowForInvalidScheme() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCenterWsUrls.toAgentWebSocketUrl("ftp://host", OpenCliCenterWebSocketPath.NODES_WS));
    }
}
