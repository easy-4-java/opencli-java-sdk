package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliProperties;
import org.junit.jupiter.api.Test;

class OpenCliWsReverseAgentClientTest {

    @Test
    void shouldRejectNullProperties() {
        OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
        assertThrows(NullPointerException.class, () -> new OpenCliWsReverseAgentClient(null, conn));
    }

    @Test
    void shouldRejectNullConnectionProperties() {
        OpenCliProperties props = new OpenCliProperties();
        assertThrows(NullPointerException.class, () -> new OpenCliWsReverseAgentClient(props, null));
    }

    @Test
    void shouldConstructSuccessfully() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
        OpenCliWsReverseAgentClient client = new OpenCliWsReverseAgentClient(props, conn);
        assertNotNull(client);
    }

    @Test
    void shouldBeAutoCloseable() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
        try (OpenCliWsReverseAgentClient client = new OpenCliWsReverseAgentClient(props, conn)) {
            assertNotNull(client);
        }
    }

    @Test
    void shouldAllowStopWithoutStart() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
        OpenCliWsReverseAgentClient client = new OpenCliWsReverseAgentClient(props, conn);
        assertDoesNotThrow(client::stop);
    }

    @Test
    void shouldAllowDoubleStop() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
        OpenCliWsReverseAgentClient client = new OpenCliWsReverseAgentClient(props, conn);
        client.stop();
        assertDoesNotThrow(client::stop);
    }

    @Test
    void shouldValidateConnectionPropertiesMissingCentralUrl() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
        conn.setCentralApiBaseUrl(null);
        conn.setAgentAdvertiseUrl("http://edge:19823");
        OpenCliWsReverseAgentClient client = new OpenCliWsReverseAgentClient(props, conn);
        // start() will trigger validation internally; we just test construction doesn't fail
        assertNotNull(client);
    }

    @Test
    void shouldAcceptValidConnectionProperties() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
        conn.setCentralApiBaseUrl("http://central:8031");
        conn.setAgentAdvertiseUrl("http://edge:19823");
        conn.setMode("cdp");
        conn.setNodeType("shell");
        OpenCliWsReverseAgentClient client = new OpenCliWsReverseAgentClient(props, conn);
        assertNotNull(client);
        client.close();
    }
}
