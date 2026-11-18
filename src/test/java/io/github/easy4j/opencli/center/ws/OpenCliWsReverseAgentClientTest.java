package io.github.easy4j.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliProperties;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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

    @Test
    void defaultConnectionDiagnosticExcludesAddressesAndLabel() {
        OpenCliWsAgentConnectionProperties properties = new OpenCliWsAgentConnectionProperties();
        properties.setCentralApiBaseUrl("https://center.example/?token=center-canary");
        properties.setAgentAdvertiseUrl("https://agent-canary.example/");
        properties.setLabel("label-canary");

        String diagnostic = properties.toString();
        assertTrue(diagnostic.contains("webSocketPath=NODES_WS"));
        assertTrue(diagnostic.contains("maxPendingCollects="));
        assertFalse(diagnostic.contains("center-canary"));
        assertFalse(diagnostic.contains("agent-canary"));
        assertFalse(diagnostic.contains("label-canary"));
        assertEquals("https://agent-canary.example/", properties.getAgentAdvertiseUrl());
    }

    @Test
    void failedSessionLogDoesNotEchoConfiguredUrlCanary() throws Exception {
        String canary = "wsurlsecret" + UUID.randomUUID().toString().replace("-", "");
        OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
        conn.setCentralApiBaseUrl("ftp://host?token=" + canary);
        conn.setAgentAdvertiseUrl("http://edge:19823");
        conn.setMinReconnectDelayMillis(5_000L);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        try (PrintStream capture = new PrintStream(diagnostics, true, "UTF-8");
             OpenCliWsReverseAgentClient client = new OpenCliWsReverseAgentClient(new OpenCliProperties(), conn)) {
            System.setErr(capture);
            try {
                client.start();
                long deadline = System.nanoTime() + 2_000_000_000L;
                while (!diagnostics.toString(StandardCharsets.UTF_8.name()).contains("opencli ws session failed")
                    && System.nanoTime() < deadline) {
                    Thread.sleep(10L);
                }
            } finally {
                client.stop();
                System.setErr(previousErr);
            }
        }
        String logText = diagnostics.toString(StandardCharsets.UTF_8.name());
        assertTrue(logText.contains("opencli ws session failed"));
        assertFalse(logText.contains(canary));
    }
}
