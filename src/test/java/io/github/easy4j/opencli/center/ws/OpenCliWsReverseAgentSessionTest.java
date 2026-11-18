package io.github.easy4j.opencli.center.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.contract.ContractProbe;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenCliWsReverseAgentSessionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String AGENT_URL = "http://127.0.0.1:19823";

    @Test
    void exchangesRegistrationAndCollectResultsAndReconnectsAfterRemoteClose() throws Exception {
        try (LocalCenter center = new LocalCenter()) {
            OpenCliWsAgentConnectionProperties connection = center.agentProperties();
            connection.setMode("");
            connection.setNodeType("");
            connection.setLabel(null);
            try (OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(probe(ContractProbe.class.getName()), connection)) {
                agent.start();
                WireMessage registration = center.receive("register");
                assertEquals(AGENT_URL, registration.body.path("agent_url").asText());
                assertEquals("cdp", registration.body.path("mode").asText());
                assertEquals("shell", registration.body.path("node_type").asText());
                assertTrue(registration.body.path("label").asText().isEmpty());

                registration.socket.send("{\"type\":\"registered\"}");
                agent.start();
                registration.socket.send("malformed-json");
                registration.socket.send("{\"type\":\"pong\"}");
                registration.socket.send("{\"type\":\"ping\"}");
                center.receive("pong");

                registration.socket.send("{\"type\":\"collect\",\"request_id\":\"valid\","
                    + "\"site\":\"demo\",\"command\":\"echo\",\"format\":\"text\","
                    + "\"positional_args\":[\"  kept  \",null,42],\"args\":{"
                    + "\"flag\":null,\"boolean\":false,\"count\":7,\"long\":2147483648,"
                    + "\"double\":1.5,\"text\":\"a b\",\"structured\":{\"key\":1}}}");
                JsonNode result = center.receive("result").body;
                assertEquals("valid", result.path("request_id").asText());
                assertTrue(result.path("success").asBoolean());
                assertTrue(result.path("error").isNull());
                List<String> argv = Arrays.stream(result.path("items").get(0).path("content").asText().split("\\R"))
                    .filter(line -> line.startsWith("arg:"))
                    .map(line -> new String(Base64.getDecoder().decode(line.substring(4)), StandardCharsets.UTF_8))
                    .collect(Collectors.toList());
                assertEquals(Arrays.asList("demo", "echo", "  kept  ", "42", "--flag", "--boolean",
                    "false", "--count", "7", "--long", "2147483648", "--double", "1.5", "--text",
                    "a b", "--structured", "{\"key\":1}", "-f", "text"), argv);

                registration.socket.send("{\"type\":\"collect\",\"request_id\":\"invalid\",\"command\":\"echo\"}");
                JsonNode invalid = center.receive("result").body;
                assertEquals("invalid", invalid.path("request_id").asText());
                assertFalse(invalid.path("success").asBoolean());
                assertTrue(invalid.path("items").isEmpty());
                assertEquals("collect execution failed", invalid.path("error").asText());

                registration.socket.close();
                WireMessage retry = center.receive("register");
                assertNotSame(registration.socket, retry.socket);
                assertEquals(registration.body, retry.body);
            }
        }
    }

    @Test
    void retriesAfterHandshakeCloseAndReturnsChildFailureToCenter() throws Exception {
        try (LocalCenter center = new LocalCenter();
             OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                 probe("io.github.easy4j.opencli.contract.MissingFixture"), center.agentProperties())) {
            agent.start();
            WireMessage first = center.receive("register");
            first.socket.close();
            WireMessage second = center.receive("register");
            assertNotSame(first.socket, second.socket);
            second.socket.send("{\"type\":\"registered\"}");
            second.socket.send("{\"type\":\"collect\",\"request_id\":\"child-failure\","
                + "\"site\":\"demo\",\"command\":\"echo\"}");
            JsonNode result = center.receive("result").body;
            assertEquals("child-failure", result.path("request_id").asText());
            assertFalse(result.path("success").asBoolean());
            assertTrue(result.path("items").isEmpty());
            assertTrue(result.path("error").asText().contains("io.github.easy4j.opencli.contract.MissingFixture"));
        }
    }

    private static OpenCliProperties probe(String mainClass) {
        OpenCliProperties properties = new OpenCliProperties();
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        properties.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath());
        properties.setLeadingArguments(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")), mainClass));
        properties.setCommandTimeoutMillis(5_000);
        return properties;
    }

    private static final class WireMessage {
        private final WebSocket socket;
        private final JsonNode body;

        private WireMessage(WebSocket socket, JsonNode body) {
            this.socket = socket;
            this.body = body;
        }
    }

    private static final class LocalCenter extends WebSocketServer implements AutoCloseable {
        private final CountDownLatch started = new CountDownLatch(1);
        private final BlockingQueue<WireMessage> messages = new LinkedBlockingQueue<>();
        private final AtomicReference<Exception> failure = new AtomicReference<>();

        private LocalCenter() throws Exception {
            super(new InetSocketAddress("127.0.0.1", 0), 1);
            start();
            if (!started.await(5, TimeUnit.SECONDS) || Objects.nonNull(failure.get())) {
                stop(1_000);
                throw new IllegalStateException("WebSocket test server failed to start", failure.get());
            }
        }

        private OpenCliWsAgentConnectionProperties agentProperties() {
            OpenCliWsAgentConnectionProperties properties = new OpenCliWsAgentConnectionProperties();
            properties.setCentralApiBaseUrl("http://127.0.0.1:" + getPort());
            properties.setAgentAdvertiseUrl(AGENT_URL);
            properties.setMinReconnectDelayMillis(25);
            properties.setMaxReconnectDelayMillis(50);
            properties.setHandshakeTimeoutMillis(5_000);
            return properties;
        }

        private WireMessage receive(String type) throws InterruptedException {
            WireMessage message = messages.poll(8, TimeUnit.SECONDS);
            assertNull(failure.get());
            assertNotNull(message, "Expected WebSocket " + type + " response");
            assertEquals(type, message.body.path("type").asText());
            return message;
        }

        @Override
        public void onOpen(WebSocket socket, ClientHandshake handshake) { }

        @Override
        public void onClose(WebSocket socket, int code, String reason, boolean remote) { }

        @Override
        public void onMessage(WebSocket socket, String payload) {
            try {
                messages.add(new WireMessage(socket, MAPPER.readTree(payload)));
            } catch (IOException e) {
                failure.compareAndSet(null, e);
            }
        }

        @Override
        public void onError(WebSocket socket, Exception error) {
            failure.compareAndSet(null, error);
            started.countDown();
        }

        @Override
        public void onStart() {
            started.countDown();
        }

        @Override
        public void close() throws InterruptedException {
            stop(1_000);
        }
    }
}
