package io.github.hiwepy.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OpenCliCenterWsUrlsTest {

    @Test
    void httpToWsAndAppendsPath() {
        assertEquals(
            "ws://192.168.0.1:8031/api/v1/nodes/ws",
            OpenCliCenterWsUrls.toAgentWebSocketUrl(
                "http://192.168.0.1:8031", OpenCliCenterWebSocketPath.NODES_WS));
    }

    @Test
    void stripsTrailingSlashOnBase() {
        assertEquals(
            "wss://api.example.com/api/v1/browsers/agents/ws",
            OpenCliCenterWsUrls.toAgentWebSocketUrl(
                "https://api.example.com///", OpenCliCenterWebSocketPath.BROWSERS_AGENTS_WS));
    }

    @Test
    void rejectsNonHttpScheme() {
        assertThrows(
            IllegalArgumentException.class,
            () ->
                OpenCliCenterWsUrls.toAgentWebSocketUrl(
                    "ftp://x", OpenCliCenterWebSocketPath.NODES_WS));
    }
}
