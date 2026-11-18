package io.github.easy4j.opencli.center.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.contract.ContractProbe;
import io.github.easy4j.opencli.contract.LifecycleProbe;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.enums.Opcode;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class OpenCliWsReverseAgentSessionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String AGENT_URL = "http://127.0.0.1:19823";

    @Test
    void stopDuringConnectionSetupDoesNotPublishAnotherGeneration() throws Exception {
        CountDownLatch resolving = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (LocalCenter center = new LocalCenter()) {
            OpenCliWsAgentConnectionProperties connection = new OpenCliWsAgentConnectionProperties() {
                @Override
                public String getCentralApiBaseUrl() {
                    resolving.countDown();
                    boolean interrupted = false;
                    while (release.getCount() > 0) {
                        try {
                            release.await();
                        } catch (InterruptedException failure) {
                            interrupted = true;
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return super.getCentralApiBaseUrl();
                }
            };
            connection.setCentralApiBaseUrl(center.agentProperties().getCentralApiBaseUrl());
            connection.setAgentAdvertiseUrl(AGENT_URL);
            try (OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                    probe(ContractProbe.class.getName()), connection)) {
                try {
                    agent.start();
                    assertTrue(resolving.await(5, TimeUnit.SECONDS));
                    agent.stop();
                } finally {
                    release.countDown();
                }
                assertNull(center.messages.poll(1, TimeUnit.SECONDS), "stopped worker published a registration");
            }
        }
    }

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
                assertEquals(5, registration.body.size());
                assertEquals(AGENT_URL, registration.body.path("agent_url").asText());
                assertEquals("cdp", registration.body.path("mode").asText());
                assertEquals("shell", registration.body.path("node_type").asText());
                assertTrue(registration.body.path("label").isTextual());
                assertTrue(registration.body.path("label").asText().isEmpty());

                registration.socket.send("{\"type\":\"registered\"}");
                agent.start();
                registration.socket.send("malformed-json");
                registration.socket.send("{\"type\":\"pong\"}");
                registration.socket.send("{\"type\":\"ping\"}");
                assertEquals(1, center.receive("pong").body.size());

                registration.socket.send("{\"type\":\"collect\",\"request_id\":\"valid\","
                    + "\"site\":\"demo\",\"command\":\"echo\",\"format\":\"text\","
                    + "\"positional_args\":[\"  kept  \",null,42],\"args\":{"
                    + "\"flag\":null,\"boolean\":false,\"count\":7,\"long\":2147483648,"
                    + "\"double\":1.5,\"text\":\"a b\",\"structured\":{\"key\":1}}}");
                JsonNode result = center.receive("result").body;
                assertEquals(5, result.size());
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
                assertEquals(5, invalid.size());
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
    void collectBeforeRegistrationAcknowledgementDoesNotStartChild(@TempDir Path temporary) throws Exception {
        Path marker = temporary.resolve("child-started");
        try (LocalCenter center = new LocalCenter();
             OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                 probe(LifecycleProbe.class.getName()), center.agentProperties())) {
            agent.start();
            WireMessage registration = center.receive("register");
            registration.socket.send(MAPPER.createObjectNode()
                .put("type", "collect").put("request_id", "early").put("site", "write")
                .put("command", marker.toString()).toString());
            JsonNode rejected = center.receive("result").body;
            assertEquals("early", rejected.path("request_id").asText());
            assertFalse(Files.exists(marker), "an unregistered collect started a child");
            assertFalse(rejected.path("success").asBoolean());
            assertEquals("worker is not registered", rejected.path("error").asText());
        }
    }

    @Test
    void trailingJsonCannotAcknowledgeRegistration(@TempDir Path temporary) throws Exception {
        Path rejectedMarker = temporary.resolve("invalid-ack-child");
        Path readyMarker = temporary.resolve("ready-child");
        try (LocalCenter center = new LocalCenter();
             OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                 probe(LifecycleProbe.class.getName()), center.agentProperties())) {
            agent.start();
            WebSocket socket = center.receive("register").socket;
            socket.send("{\"type\":\"registered\"} {\"type\":\"ping\"}");
            socket.send(collect("invalid-ack", "write", rejectedMarker).toString());
            JsonNode rejected = center.receive("result").body;
            assertFalse(rejected.path("success").asBoolean(), "malformed acknowledgement enabled collection");
            assertFalse(Files.exists(rejectedMarker));

            socket.send("{\"type\":\"registered\"}");
            socket.send(collect("valid-ack", "write", readyMarker).toString());
            assertTrue(center.receive("result").body.path("success").asBoolean());
            assertTrue(Files.exists(readyMarker));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oversizedMessageRetiresGenerationBeforeCollect(boolean fragmented, @TempDir Path temporary) throws Exception {
        Path marker = temporary.resolve("oversized-child");
        char[] padding = new char[1024 * 1024];
        Arrays.fill(padding, 'x');
        byte[] bytes = collect("oversized", "write", marker).put("fixture", new String(padding))
            .toString().getBytes(StandardCharsets.UTF_8);
        try (LocalCenter center = new LocalCenter();
             OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                 probe(LifecycleProbe.class.getName()), center.agentProperties())) {
            agent.start();
            WebSocket socket = center.receive("register").socket;
            socket.send("{\"type\":\"registered\"}");
            if (fragmented) {
                int midpoint = bytes.length / 2;
                socket.sendFragmentedFrame(Opcode.TEXT, ByteBuffer.wrap(bytes, 0, midpoint).slice(), false);
                socket.sendFragmentedFrame(Opcode.TEXT,
                    ByteBuffer.wrap(bytes, midpoint, bytes.length - midpoint).slice(), true);
            } else {
                socket.send(new String(bytes, StandardCharsets.UTF_8));
            }
            Integer code = center.closeCodes.poll(3, TimeUnit.SECONDS);
            // Rejecting a complete frame from its header can reset the peer's still-active write.
            assertTrue(Integer.valueOf(1009).equals(code) || (!fragmented && Integer.valueOf(1006).equals(code)),
                "oversized message should close the socket, observed code=" + code);
            assertFalse(Files.exists(marker), "oversized request started a child");
            assertNotSame(socket, center.receive("register").socket);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MAX_VALUE})
    void rejectsUnboundedInboundLimitBeforeConnection(int limit) {
        OpenCliWsAgentConnectionProperties connection = new OpenCliWsAgentConnectionProperties();
        connection.setMaxInboundMessageBytes(limit);
        assertThrows(IllegalArgumentException.class, () -> new OpenCliWsReverseAgentClient(
            probe(LifecycleProbe.class.getName()), connection));
    }

    @Test
    void disconnectAndCloseReleaseBoundedWorkerResources(@TempDir Path temporary) throws Exception {
        Set<Thread> baseline = Thread.getAllStackTraces().keySet();
        Path gate = temporary.resolve("resource-release");
        Path queued = temporary.resolve("resource-queued");
        Path overflow = temporary.resolve("resource-overflow");
        List<Path> pulses = Arrays.asList(temporary.resolve("heartbeat-0"), temporary.resolve("heartbeat-1"));
        List<Path> starts = Arrays.asList(temporary.resolve("start-0"), temporary.resolve("start-1"));
        try (LocalCenter center = new LocalCenter()) {
            OpenCliWsAgentConnectionProperties connection = center.agentProperties();
            connection.setMaxConcurrentCollects(2);
            connection.setMaxPendingCollects(1);
            OpenCliProperties cli = probe(LifecycleProbe.class.getName());
            cli.setCommandTimeoutMillis(20_000L);
            try (OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(cli, connection)) {
                agent.start();
                WebSocket first = center.receive("register").socket;
                first.send("{\"type\":\"registered\"}");
                try {
                    for (int index = 0; index < pulses.size(); index++) {
                        ObjectNode payload = collect("resource-" + index, "heartbeat-log", pulses.get(index));
                        payload.putArray("positional_args").add(gate.toString()).add(starts.get(index).toString());
                        first.send(payload.toString());
                        assertTrue(awaitMarker(pulses.get(index), 5_000L));
                    }
                    first.send(collect("queued-resource", "write", queued).toString());
                    first.send(collect("overflow-resource", "write", overflow).toString());
                    JsonNode rejected = center.receive("result").body;
                    assertEquals("overflow-resource", rejected.path("request_id").asText());
                    assertEquals("worker is busy", rejected.path("error").asText());
                    assertFalse(Files.exists(queued));
                    assertFalse(Files.exists(overflow));

                    first.close();
                    WebSocket second = center.receive("register").socket;
                    assertNotSame(first, second);
                    second.send("{\"type\":\"registered\"}");
                    second.send("{\"type\":\"ping\"}");
                    center.receive("pong");

                    Set<Thread> running = workerThreadsSince(baseline);
                    assertEquals(2, running.stream().filter(t -> "opencli-ws-collect".equals(t.getName())).count());
                    assertEquals(1, running.stream().filter(t -> "opencli-ws-agent-runner".equals(t.getName())).count());
                    assertEquals(2, running.stream().filter(t -> "opencli-stdout".equals(t.getName())).count());
                    assertEquals(2, running.stream().filter(t -> "opencli-stderr".equals(t.getName())).count());

                    agent.close();
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
                    while (!workerThreadsSince(baseline).isEmpty() && System.nanoTime() < deadline) {
                        Thread.sleep(20L);
                    }
                    assertTrue(workerThreadsSince(baseline).isEmpty(), "worker threads survived close");
                    byte[] firstPulse = Files.readAllBytes(pulses.get(0));
                    byte[] secondPulse = Files.readAllBytes(pulses.get(1));
                    Thread.sleep(300L);
                    assertTrue(Arrays.equals(firstPulse, Files.readAllBytes(pulses.get(0))));
                    assertTrue(Arrays.equals(secondPulse, Files.readAllBytes(pulses.get(1))));
                    for (Path started : starts) {
                        assertEquals(Arrays.asList("started"), Files.readAllLines(started, StandardCharsets.UTF_8));
                    }
                    assertFalse(Files.exists(queued));
                    assertFalse(Files.exists(overflow));
                    System.out.println("C05 resources: started=2 workerThreads=2 runnerThreads=1 readers=4 threadsAfterClose=0");
                } finally {
                    Files.createFile(gate);
                }
            }
        }
    }

    private static Set<Thread> workerThreadsSince(Set<Thread> baseline) {
        List<String> names = Arrays.asList("opencli-ws-collect", "opencli-ws-agent-runner",
            "opencli-stdout", "opencli-stderr", "opencli-stop", "opencli-force-stop");
        return Thread.getAllStackTraces().keySet().stream()
            .filter(thread -> !baseline.contains(thread) && names.contains(thread.getName()))
            .collect(Collectors.toSet());
    }

    @Test
    void defaultsToFourActiveCollects(@TempDir Path temporary) throws Exception {
        Path release = temporary.resolve("release");
        OpenCliProperties properties = probe(LifecycleProbe.class.getName());
        properties.setMaxConcurrentExecutions(8);
        properties.setCommandTimeoutMillis(10_000);
        try (LocalCenter center = new LocalCenter();
             OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(properties, center.agentProperties())) {
            agent.start();
            WebSocket socket = center.receive("register").socket;
            socket.send("{\"type\":\"registered\"}");
            try {
                for (int index = 0; index < 4; index++) {
                    sendHeldCollect(socket, "collect-" + index, temporary.resolve("child-" + index), release);
                }
                for (int index = 0; index < 4; index++) {
                    assertTrue(awaitMarker(temporary.resolve("child-" + index), 5_000), "initial child did not start");
                }
                Path queued = temporary.resolve("child-4");
                sendHeldCollect(socket, "collect-4", queued, release);
                assertFalse(awaitMarker(queued, 1_250), "a fifth collect started before a worker was free");
            } finally {
                Files.createFile(release);
            }
            Set<String> completed = new HashSet<>();
            for (int index = 0; index < 5; index++) {
                JsonNode result = center.receive("result").body;
                assertTrue(result.path("success").asBoolean(), result::toString);
                completed.add(result.path("request_id").asText());
            }
            assertEquals(new HashSet<>(Arrays.asList("collect-0", "collect-1", "collect-2", "collect-3", "collect-4")),
                completed);
            assertTrue(Files.exists(temporary.resolve("child-4")));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void rejectsOverflowWithoutLaunchingAnotherChild(int queueCapacity, @TempDir Path temporary) throws Exception {
        Path release = temporary.resolve("release");
        Path active = temporary.resolve("active");
        Path queued = temporary.resolve("queued");
        Path rejected = temporary.resolve("rejected");
        try (LocalCenter center = new LocalCenter()) {
            OpenCliWsAgentConnectionProperties connection = center.agentProperties();
            connection.setMaxConcurrentCollects(1);
            connection.setMaxPendingCollects(queueCapacity);
            try (OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                probe(LifecycleProbe.class.getName()), connection)) {
                agent.start();
                WebSocket socket = center.receive("register").socket;
                socket.send("{\"type\":\"registered\"}");
                try {
                    sendHeldCollect(socket, "active", active, release);
                    assertTrue(awaitMarker(active, 3_000), "first collect did not start");
                    if (queueCapacity > 0) {
                        sendHeldCollect(socket, "queued", queued, release);
                    }
                    sendHeldCollect(socket, "overflow", rejected, release);
                    JsonNode busy = center.receive("result").body;
                    assertEquals("overflow", busy.path("request_id").asText());
                    assertFalse(busy.path("success").asBoolean());
                    assertEquals("worker is busy", busy.path("error").asText());
                    assertFalse(Files.exists(rejected));
                    assertFalse(Files.exists(queued));
                } finally {
                    Files.createFile(release);
                }
                JsonNode completed = center.receive("result").body;
                assertEquals("active", completed.path("request_id").asText());
                assertTrue(completed.path("success").asBoolean());
                if (queueCapacity > 0) {
                    JsonNode drained = center.receive("result").body;
                    assertEquals("queued", drained.path("request_id").asText());
                    assertTrue(drained.path("success").asBoolean());
                    assertTrue(Files.exists(queued));
                }
                assertFalse(Files.exists(rejected));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"0,32", "-1,32", "4,-1", "4,2147483647", "4,2147483644"})
    void rejectsInvalidCapacityBeforeConnection(int workers, int pending) {
        OpenCliWsAgentConnectionProperties connection = new OpenCliWsAgentConnectionProperties();
        connection.setMaxConcurrentCollects(workers);
        connection.setMaxPendingCollects(pending);
        assertThrows(IllegalArgumentException.class,
            () -> new OpenCliWsReverseAgentClient(new OpenCliProperties(), connection));
    }

    @Test
    void restartableStopDropsQueuedCollectsAndCancelsPastDrainDeadline(@TempDir Path temporary) throws Exception {
        Path release = temporary.resolve("stop-release");
        Path heartbeat = temporary.resolve("stop-heartbeat");
        Path queued = temporary.resolve("stop-queued");
        Path restarted = temporary.resolve("stop-restarted");
        try (LocalCenter center = new LocalCenter()) {
            OpenCliWsAgentConnectionProperties connection = center.agentProperties();
            connection.setMaxConcurrentCollects(1);
            connection.setMaxPendingCollects(1);
            OpenCliProperties cli = probe(LifecycleProbe.class.getName());
            cli.setCommandTimeoutMillis(20_000L);
            try (OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(cli, connection)) {
                agent.start();
                WebSocket first = center.receive("register").socket;
                first.send("{\"type\":\"registered\"}");
                try {
                    ObjectNode active = collect("active-stop", "heartbeat", heartbeat);
                    active.putArray("positional_args").add(release.toString());
                    first.send(active.toString());
                    assertTrue(awaitMarker(heartbeat, 3_000L), "first collect did not start");
                    sendHeldCollect(first, "queued-stop", queued, release);
                    first.send("{\"type\":\"ping\"}");
                    center.receive("pong");

                    long stopStarted = System.nanoTime();
                    agent.stop();
                    assertTrue(System.nanoTime() - stopStarted < TimeUnit.SECONDS.toNanos(7));
                    assertFalse(awaitMarker(queued, 500L), "queued collect started after stop");
                    byte[] before = Files.readAllBytes(heartbeat);
                    Thread.sleep(300L);
                    assertTrue(Arrays.equals(before, Files.readAllBytes(heartbeat)), "active collect kept running after stop");

                    agent.start();
                    WebSocket second = center.receive("register").socket;
                    assertNotSame(first, second);
                    second.send("{\"type\":\"registered\"}");
                    second.send(collect("queued-stop", "write", restarted).toString());
                    assertTrue(center.receive("result").body.path("success").asBoolean());
                    assertTrue(Files.exists(restarted));
                } finally {
                    Files.createFile(release);
                }
            }
        }
    }

    @Test
    void closeDropsQueuedCollectsAndRejectsRestart(@TempDir Path temporary) throws Exception {
        Path release = temporary.resolve("release");
        Path active = temporary.resolve("active");
        Path queued = temporary.resolve("queued");
        try (LocalCenter center = new LocalCenter()) {
            OpenCliWsAgentConnectionProperties connection = center.agentProperties();
            connection.setMaxConcurrentCollects(1);
            connection.setMaxPendingCollects(1);
            try (OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                probe(LifecycleProbe.class.getName()), connection)) {
                agent.start();
                WebSocket socket = center.receive("register").socket;
                socket.send("{\"type\":\"registered\"}");
                try {
                    sendHeldCollect(socket, "active", active, release);
                    assertTrue(awaitMarker(active, 3_000), "first collect did not start");
                    sendHeldCollect(socket, "queued", queued, release);
                    socket.send("{\"type\":\"ping\"}");
                    center.receive("pong");
                    agent.close();
                } finally {
                    Files.createFile(release);
                }
                assertFalse(awaitMarker(queued, 1_250), "queued collect started after close");
                agent.close();
                assertThrows(IllegalStateException.class, agent::start);
            }
        }
    }

    @Test
    void duplicateRequestRunsOnceAndReplaysCompletedResult(@TempDir Path temporary) throws Exception {
        Path marker = temporary.resolve("starts");
        Path release = temporary.resolve("release");
        Path changed = temporary.resolve("changed");
        String payload = collect("same", "count-hold", marker)
            .set("positional_args", MAPPER.createArrayNode().add(release.toString())).toString();
        try (LocalCenter center = new LocalCenter();
             OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                 probe(LifecycleProbe.class.getName()), center.agentProperties())) {
            agent.start();
            WebSocket socket = center.receive("register").socket;
            socket.send("{\"type\":\"registered\"}");
            try {
                socket.send(payload);
                assertTrue(awaitMarker(marker, 3_000), "original child did not start");
                socket.send(payload);
                socket.send(collect("same", "write", changed).toString());
                JsonNode conflict = center.receive("result").body;
                assertFalse(Files.exists(changed), "conflicting request started a child");
                assertFalse(conflict.path("success").asBoolean());
                assertEquals("worker request ID conflict", conflict.path("error").asText());
            } finally {
                Files.createFile(release);
            }
            JsonNode completed = center.receive("result").body;
            assertTrue(completed.path("success").asBoolean());
            assertFalse(completed.has("cached"));
            socket.send(payload);
            JsonNode replayed = center.receive("result").body;
            assertTrue(replayed.path("cached").asBoolean());
            assertEquals(completed.path("items"), replayed.path("items"));
            assertEquals(Arrays.asList("started"), Files.readAllLines(marker, StandardCharsets.UTF_8));
        }
    }

    @Test
    void rejectsInvalidRequestIdsBeforeExecution(@TempDir Path temporary) throws Exception {
        Path marker = temporary.resolve("invalid-id-start");
        try (LocalCenter center = new LocalCenter();
             OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                 probe(LifecycleProbe.class.getName()), center.agentProperties())) {
            agent.start();
            WebSocket socket = center.receive("register").socket;
            socket.send("{\"type\":\"registered\"}");
            for (JsonNode invalid : Arrays.asList(MAPPER.nullNode(), MAPPER.getNodeFactory().textNode(" 　"),
                MAPPER.getNodeFactory().numberNode(42), MAPPER.getNodeFactory().textNode(String.join("", Collections.nCopies(43, "界"))))) {
                ObjectNode payload = collect("unused", "write", marker);
                payload.set("request_id", invalid);
                socket.send(payload.toString());
                JsonNode reply = center.receive("result").body;
                assertEquals("", reply.path("request_id").asText());
                assertFalse(reply.path("success").asBoolean());
                assertEquals("invalid worker request ID", reply.path("error").asText());
                assertFalse(Files.exists(marker));
            }
            String limit = String.join("", Collections.nCopies(42, "界")) + "ab";
            socket.send(collect(limit, "write", marker).toString());
            JsonNode accepted = center.receive("result").body;
            assertEquals(limit, accepted.path("request_id").asText());
            assertTrue(accepted.path("success").asBoolean());
            assertTrue(Files.exists(marker));
        }
    }

    @Test
    void plainWebSocketReconnectDoesNotExposePreviousSessionCache(@TempDir Path temporary) throws Exception {
        verifyReconnectReplay(temporary, null, false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void verifiedTlsReconnectReplaysOnlyAfterAnExplicitRetry(boolean failDelivery, @TempDir Path temporary) throws Exception {
        Path storePath = temporary.resolve("center.p12");
        String keytool = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        Path tool = Paths.get(System.getProperty("java.home"), "bin", keytool);
        Process keygen = new ProcessBuilder(tool.toString(), "-genkeypair", "-alias", "center", "-keyalg", "RSA",
            "-keysize", "2048", "-dname", "CN=localhost", "-ext", "SAN=IP:127.0.0.1", "-validity", "1",
            "-storetype", "PKCS12", "-keystore", storePath.toString(), "-storepass", "test-password")
            .redirectErrorStream(true).redirectOutput(temporary.resolve("keytool.log").toFile()).start();
        try {
            assertTrue(keygen.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, keygen.exitValue());
        } finally {
            if (keygen.isAlive()) {
                keygen.destroyForcibly();
            }
        }
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(storePath)) {
            store.load(input, "test-password".toCharArray());
        }
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, "test-password".toCharArray());
        SSLContext server = SSLContext.getInstance("TLS");
        server.init(keys.getKeyManagers(), null, null);
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        SSLContext trustedClient = SSLContext.getInstance("TLS");
        trustedClient.init(null, trust.getTrustManagers(), null);
        SSLContext previous = SSLContext.getDefault();
        try {
            SSLContext.setDefault(trustedClient);
            if (failDelivery) {
                verifyFailedDeliveryReplay(temporary, server);
            } else {
                verifyReconnectReplay(temporary, server, true);
            }
        } finally {
            SSLContext.setDefault(previous);
        }
    }

    private static void verifyFailedDeliveryReplay(Path temporary, SSLContext tls) throws Exception {
        Path marker = temporary.resolve("delivery-start");
        Path release = Files.createFile(temporary.resolve("delivery-release"));
        ObjectNode request = collect("delivery", "count-hold", marker);
        request.putArray("positional_args").add(release.toString());
        String canary = "delivery-exception-private-canary";
        AtomicBoolean firstSend = new AtomicBoolean(true);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        try (LocalCenter center = new LocalCenter(tls);
             OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                 probe(LifecycleProbe.class.getName()), center.agentProperties(), (socket, json) -> {
                     if (firstSend.getAndSet(false)) {
                         throw new IllegalStateException(canary);
                     }
                     socket.send(json);
                 });
             PrintStream capture = new PrintStream(diagnostics, true, "UTF-8")) {
            System.setErr(capture);
            try {
                agent.start();
                WebSocket first = center.receive("register").socket;
                first.send("{\"type\":\"registered\"}");
                first.send(request.toString());

                WebSocket second = center.receive("register").socket;
                assertNotSame(first, second);
                second.send("{\"type\":\"registered\"}");
                second.send("{\"type\":\"ping\"}");
                center.receive("pong");
                assertEquals(Arrays.asList("started"), Files.readAllLines(marker, StandardCharsets.UTF_8));

                second.send(request.toString());
                JsonNode replayed = center.receive("result").body;
                assertEquals("delivery", replayed.path("request_id").asText());
                assertTrue(replayed.path("success").asBoolean());
                assertTrue(replayed.path("cached").asBoolean());
                assertTrue(replayed.path("error").isNull());
                assertEquals(Arrays.asList("started"), Files.readAllLines(marker, StandardCharsets.UTF_8));
            } finally {
                System.setErr(previousErr);
            }
        }
        String logText = diagnostics.toString(StandardCharsets.UTF_8.name());
        assertTrue(logText.contains("failed to send WS result"));
        assertFalse(logText.contains(canary));
    }

    private static void verifyReconnectReplay(Path temporary, SSLContext tls, boolean expectCache) throws Exception {
        Path marker = temporary.resolve("reconnect-start");
        Path release = temporary.resolve("reconnect-release");
        ObjectNode request = collect("resend", "count-hold", marker);
        request.putArray("positional_args").add(release.toString());
        try (LocalCenter center = new LocalCenter(tls)) {
            OpenCliWsAgentConnectionProperties connection = center.agentProperties();
            connection.setMaxConcurrentCollects(1);
            try (OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(
                probe(LifecycleProbe.class.getName()), connection)) {
                agent.start();
                WebSocket first = center.receive("register").socket;
                first.send("{\"type\":\"registered\"}");
                try {
                    first.send(request.toString());
                    assertTrue(awaitMarker(marker, 3_000));
                    first.close();
                    WebSocket second = center.receive("register").socket;
                    second.send("{\"type\":\"registered\"}");
                    second.send("{\"type\":\"ping\"}");
                    center.receive("pong");
                    Files.createFile(release);
                    second.send(collect("barrier", "write", temporary.resolve("barrier")).toString());
                    JsonNode barrier = center.receive("result").body;
                    assertEquals("barrier", barrier.path("request_id").asText());
                    assertTrue(barrier.path("success").asBoolean());
                    assertEquals(Arrays.asList("started"), Files.readAllLines(marker, StandardCharsets.UTF_8));

                    second.send(request.toString());
                    JsonNode reply = center.receive("result").body;
                    assertTrue(reply.path("success").asBoolean(), reply::toString);
                    assertEquals(expectCache, reply.path("cached").asBoolean());
                    assertEquals(expectCache ? Arrays.asList("started") : Arrays.asList("started", "started"),
                        Files.readAllLines(marker, StandardCharsets.UTF_8));
                } finally {
                    if (!Files.exists(release)) {
                        Files.createFile(release);
                    }
                }
            }
        }
    }

    private static ObjectNode collect(String id, String command, Path marker) {
        return MAPPER.createObjectNode().put("type", "collect").put("request_id", id)
            .put("site", command).put("command", marker.toString());
    }

    private static void sendHeldCollect(WebSocket socket, String id, Path marker, Path release) {
        ObjectNode message = MAPPER.createObjectNode()
            .put("type", "collect").put("request_id", id).put("site", "hold").put("command", marker.toString());
        message.putArray("positional_args").add(release.toString());
        socket.send(message.toString());
    }

    private static boolean awaitMarker(Path marker, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (!Files.exists(marker) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return Files.exists(marker);
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
        private final BlockingQueue<Integer> closeCodes = new LinkedBlockingQueue<>();
        private final AtomicReference<Exception> failure = new AtomicReference<>();

        private final boolean secure;

        private LocalCenter() throws Exception {
            this(null);
        }

        private LocalCenter(SSLContext tls) throws Exception {
            super(new InetSocketAddress("127.0.0.1", 0), 1);
            this.secure = Objects.nonNull(tls);
            if (secure) {
                setWebSocketFactory(new DefaultSSLWebSocketServerFactory(tls));
            }
            start();
            if (!started.await(5, TimeUnit.SECONDS) || Objects.nonNull(failure.get())) {
                stop(1_000);
                throw new IllegalStateException("WebSocket test server failed to start", failure.get());
            }
        }

        private OpenCliWsAgentConnectionProperties agentProperties() {
            OpenCliWsAgentConnectionProperties properties = new OpenCliWsAgentConnectionProperties();
            properties.setCentralApiBaseUrl((secure ? "https" : "http") + "://127.0.0.1:" + getPort());
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
        public void onClose(WebSocket socket, int code, String reason, boolean remote) {
            closeCodes.add(code);
        }

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
