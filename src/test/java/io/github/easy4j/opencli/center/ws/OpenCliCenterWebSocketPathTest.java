package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliCenterWebSocketPathTest {

    @Test
    void shouldHaveTwoValues() {
        assertEquals(2, OpenCliCenterWebSocketPath.values().length);
    }

    @Test
    void nodesWsShouldHaveCorrectPath() {
        assertEquals("/api/v1/nodes/ws", OpenCliCenterWebSocketPath.NODES_WS.getPath());
    }

    @Test
    void browsersAgentsWsShouldHaveCorrectPath() {
        assertEquals("/api/v1/browsers/agents/ws", OpenCliCenterWebSocketPath.BROWSERS_AGENTS_WS.getPath());
    }
}
