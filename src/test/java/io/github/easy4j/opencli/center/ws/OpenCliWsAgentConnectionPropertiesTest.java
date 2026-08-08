package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliWsAgentConnectionPropertiesTest {

    @Test
    void shouldHaveDefaults() {
        OpenCliWsAgentConnectionProperties p = new OpenCliWsAgentConnectionProperties();
        assertEquals(OpenCliCenterWebSocketPath.NODES_WS, p.getWebSocketPath());
        assertEquals("cdp", p.getMode());
        assertEquals("", p.getLabel());
        assertEquals("shell", p.getNodeType());
        assertEquals(15_000L, p.getHandshakeTimeoutMillis());
        assertEquals(3_000L, p.getMinReconnectDelayMillis());
        assertEquals(60_000L, p.getMaxReconnectDelayMillis());
    }

    @Test
    void shouldSetAndGet() {
        OpenCliWsAgentConnectionProperties p = new OpenCliWsAgentConnectionProperties();
        p.setCentralApiBaseUrl("http://host:8031");
        assertEquals("http://host:8031", p.getCentralApiBaseUrl());
        p.setAgentAdvertiseUrl("http://edge:19823");
        assertEquals("http://edge:19823", p.getAgentAdvertiseUrl());
        p.setWebSocketPath(OpenCliCenterWebSocketPath.BROWSERS_AGENTS_WS);
        assertEquals(OpenCliCenterWebSocketPath.BROWSERS_AGENTS_WS, p.getWebSocketPath());
        p.setMode("bridge");
        assertEquals("bridge", p.getMode());
        p.setLabel("my-agent");
        assertEquals("my-agent", p.getLabel());
        p.setNodeType("docker");
        assertEquals("docker", p.getNodeType());
    }
}
