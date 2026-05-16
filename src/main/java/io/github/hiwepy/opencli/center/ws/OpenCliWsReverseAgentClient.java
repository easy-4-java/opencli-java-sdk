package io.github.hiwepy.opencli.center.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.hiwepy.opencli.OpenCliProperties;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.exception.OpenCliException;
import io.github.hiwepy.opencli.exception.OpenCliNonZeroExitException;
import io.github.hiwepy.opencli.util.OpenCliStrings;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * 边缘侧反向 WebSocket 客户端（基于 Java-WebSocket，JDK 8+ 可用）。
 */
@Slf4j
public final class OpenCliWsReverseAgentClient implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OpenCliExecutor localExecutor;

    private final OpenCliWsAgentConnectionProperties connectionProperties;

    private final ExecutorService collectPool =
        Executors.newCachedThreadPool(
            r -> {
                Thread t = new Thread(r, "opencli-ws-collect");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean shutdownRequested;

    private volatile WebSocketClient activeClient;

    private Thread runnerThread;

    /**
     * @param openCliProperties    本地 opencli 配置
     * @param connectionProperties 中心连接参数
     */
    public OpenCliWsReverseAgentClient(
        OpenCliProperties openCliProperties, OpenCliWsAgentConnectionProperties connectionProperties) {
        Objects.requireNonNull(openCliProperties, "openCliProperties");
        this.connectionProperties =
            Objects.requireNonNull(connectionProperties, "connectionProperties");
        this.localExecutor = new OpenCliExecutor(openCliProperties.copyForLocalCliExecution());
    }

    /**
     * 启动后台重连线程。
     */
    public synchronized void start() {
        if (runnerThread != null && runnerThread.isAlive()) {
            return;
        }
        shutdownRequested = false;
        runnerThread = new Thread(this::runLoop, "opencli-ws-agent-runner");
        runnerThread.setDaemon(true);
        runnerThread.start();
    }

    /**
     * 请求停止并关闭当前 WebSocket。
     */
    public synchronized void stop() {
        shutdownRequested = true;
        WebSocketClient client = activeClient;
        activeClient = null;
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("ws close ignored: {}", e.getMessage());
            }
        }
        if (runnerThread != null) {
            runnerThread.interrupt();
        }
    }

    @Override
    public void close() {
        stop();
        collectPool.shutdown();
    }

    private void runLoop() {
        int attempt = 0;
        while (!shutdownRequested) {
            attempt++;
            try {
                runOneSession();
                attempt = 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("opencli ws agent runner interrupted");
                break;
            } catch (Exception e) {
                if (!shutdownRequested) {
                    log.warn("opencli ws session failed (attempt {}): {}", attempt, e.getMessage());
                }
            }
            if (shutdownRequested) {
                break;
            }
            long backoff =
                Math.min(
                    connectionProperties.getMaxReconnectDelayMillis(),
                    attempt * connectionProperties.getMinReconnectDelayMillis());
            backoff = Math.max(backoff, connectionProperties.getMinReconnectDelayMillis());
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("opencli ws agent runner stopped");
    }

    private void runOneSession() throws Exception {
        validateConnectionProperties();
        String wsUrl =
            OpenCliCenterWsUrls.toAgentWebSocketUrl(
                connectionProperties.getCentralApiBaseUrl(), connectionProperties.getWebSocketPath());

        final CountDownLatch registered = new CountDownLatch(1);
        final CountDownLatch sessionClosed = new CountDownLatch(1);
        final AtomicReference<Throwable> handshakeFailure = new AtomicReference<>();

        log.info("opencli ws connecting: {}", wsUrl);
        WebSocketClient client =
            new WebSocketClient(URI.create(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    try {
                        send(buildRegisterJson());
                    } catch (Exception e) {
                        handshakeFailure.set(e);
                        registered.countDown();
                        close();
                    }
                }

                @Override
                public void onMessage(String message) {
                    try {
                        handleTextMessage(this, message, registered, handshakeFailure);
                    } catch (Exception e) {
                        log.warn("handle ws message failed: {}", e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (registered.getCount() > 0) {
                        handshakeFailure.compareAndSet(
                            null,
                            new IllegalStateException(
                                "WebSocket closed before 'registered' (code="
                                    + code
                                    + " reason="
                                    + reason
                                    + ")"));
                        registered.countDown();
                    }
                    sessionClosed.countDown();
                }

                @Override
                public void onError(Exception ex) {
                    log.warn("WebSocket error: {}", ex.toString());
                    if (registered.getCount() > 0) {
                        handshakeFailure.compareAndSet(null, ex);
                        registered.countDown();
                    }
                    sessionClosed.countDown();
                }
            };

        activeClient = client;
        if (!client.connectBlocking(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("WebSocket connectBlocking returned false: " + wsUrl);
        }

        if (!registered.await(connectionProperties.getHandshakeTimeoutMillis(), TimeUnit.MILLISECONDS)) {
            client.close();
            throw new IllegalStateException("Timeout waiting for 'registered' from center");
        }
        Throwable hf = handshakeFailure.get();
        if (hf != null) {
            client.close();
            if (hf instanceof Exception) {
                throw (Exception) hf;
            }
            throw new IllegalStateException(hf);
        }

        sessionClosed.await();
        activeClient = null;
    }

    private void validateConnectionProperties() {
        if (OpenCliStrings.isBlank(connectionProperties.getCentralApiBaseUrl())) {
            throw new IllegalStateException("centralApiBaseUrl must be set");
        }
        if (OpenCliStrings.isBlank(connectionProperties.getAgentAdvertiseUrl())) {
            throw new IllegalStateException("agentAdvertiseUrl must be set");
        }
        if (!connectionProperties.getAgentAdvertiseUrl().trim().startsWith("http")) {
            throw new IllegalStateException("agentAdvertiseUrl must be an http(s) URL");
        }
        String mode = connectionProperties.getMode();
        if (mode != null && !mode.isEmpty() && !"bridge".equals(mode) && !"cdp".equals(mode)) {
            throw new IllegalStateException("mode must be 'bridge' or 'cdp'");
        }
        if (connectionProperties.getWebSocketPath() == OpenCliCenterWebSocketPath.NODES_WS) {
            String nt = connectionProperties.getNodeType();
            if (nt != null
                && !nt.isEmpty()
                && !"docker".equalsIgnoreCase(nt)
                && !"shell".equalsIgnoreCase(nt)) {
                throw new IllegalStateException("For NODES_WS, nodeType must be 'docker' or 'shell'");
            }
        }
    }

    private String buildRegisterJson() throws Exception {
        ObjectNode reg = MAPPER.createObjectNode();
        reg.put("type", "register");
        reg.put("agent_url", connectionProperties.getAgentAdvertiseUrl().trim());
        reg.put(
            "mode",
            OpenCliStrings.isBlank(connectionProperties.getMode())
                ? "cdp"
                : connectionProperties.getMode().trim());
        reg.put("label", connectionProperties.getLabel() == null ? "" : connectionProperties.getLabel());
        reg.put(
            "node_type",
            OpenCliStrings.isBlank(connectionProperties.getNodeType())
                ? "shell"
                : connectionProperties.getNodeType().trim());
        return MAPPER.writeValueAsString(reg);
    }

    private void handleTextMessage(
        WebSocketClient ws,
        String payload,
        CountDownLatch registered,
        AtomicReference<Throwable> handshakeFailure)
        throws Exception {
        JsonNode root = MAPPER.readTree(payload);
        String type = root.path("type").asText("");
        if ("registered".equals(type)) {
            registered.countDown();
            return;
        }
        if ("ping".equals(type)) {
            ws.send("{\"type\":\"pong\"}");
            return;
        }
        if ("pong".equals(type)) {
            return;
        }
        if ("collect".equals(type)) {
            final JsonNode msg = root;
            collectPool.submit(() -> runCollectSafe(ws, msg));
        }
    }

    private void runCollectSafe(WebSocketClient ws, JsonNode msg) {
        String requestId = msg.path("request_id").asText("");
        try {
            String site = msg.path("site").asText("");
            String command = msg.path("command").asText("");
            String format = msg.path("format").asText("json");
            Map<String, Object> args = readArgsMap(msg.get("args"));
            List<String> positional = readPositionalList(msg.get("positional_args"));

            List<String> argv =
                OpenCliCenterWsCollectToArgv.toArgv(site, command, positional, args, format);
            OpenCliResult r = localExecutor.invoke(argv);
            List<JsonNode> items = OpenCliWsCollectStdoutItems.parseItems(r.getStdout(), format);
            if (!r.isSuccess()) {
                sendResult(ws, requestId, false, items, "opencli reported failure");
                return;
            }
            sendResult(ws, requestId, true, items, null);
        } catch (OpenCliNonZeroExitException e) {
            OpenCliResult partial = e.getPartialResult();
            String err =
                partial != null && OpenCliStrings.isNotBlank(partial.getStderr())
                    ? partial.getStderr()
                    : e.getMessage();
            sendResult(ws, requestId, false, Collections.<JsonNode>emptyList(), err);
        } catch (OpenCliException e) {
            OpenCliResult partial = e.getPartialResult();
            String err =
                partial != null && OpenCliStrings.isNotBlank(partial.getStderr())
                    ? partial.getStderr()
                    : e.getMessage();
            sendResult(ws, requestId, false, Collections.<JsonNode>emptyList(), err);
        } catch (Exception e) {
            log.warn("collect execution error request_id={}: {}", requestId, e.getMessage());
            sendResult(ws, requestId, false, Collections.<JsonNode>emptyList(), e.getMessage());
        }
    }

    private static Map<String, Object> readArgsMap(JsonNode argsNode) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (argsNode == null || !argsNode.isObject()) {
            return m;
        }
        Iterator<Map.Entry<String, JsonNode>> it = argsNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            m.put(e.getKey(), readArgValue(e.getValue()));
        }
        return m;
    }

    private static Object readArgValue(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isBoolean()) {
            return n.booleanValue();
        }
        if (n.isInt()) {
            return n.intValue();
        }
        if (n.isLong()) {
            return n.longValue();
        }
        if (n.isDouble() || n.isFloat()) {
            return n.doubleValue();
        }
        if (n.isTextual()) {
            return n.asText();
        }
        return n.toString();
    }

    private static List<String> readPositionalList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode n : node) {
            if (n != null && !n.isNull()) {
                list.add(n.asText());
            }
        }
        return list;
    }

    private static void sendResult(
        WebSocketClient ws, String requestId, boolean success, List<JsonNode> items, String error) {
        try {
            ObjectNode out = MAPPER.createObjectNode();
            out.put("type", "result");
            out.put("request_id", requestId);
            out.put("success", success);
            ArrayNode arr = MAPPER.createArrayNode();
            if (items != null) {
                for (JsonNode i : items) {
                    arr.add(i);
                }
            }
            out.set("items", arr);
            if (error == null || error.isEmpty()) {
                out.putNull("error");
            } else {
                out.put("error", error);
            }
            ws.send(MAPPER.writeValueAsString(out));
        } catch (Exception e) {
            log.error("failed to send WS result request_id={}: {}", requestId, e.getMessage());
        }
    }
}
