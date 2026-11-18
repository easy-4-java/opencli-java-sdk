package io.github.easy4j.opencli.center.ws;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * 边缘侧反向 WebSocket 客户端（基于 Java-WebSocket，JDK 8+ 可用）。
 */
@Slf4j/**

 * Edge-side reverse WebSocket client (based on Java-WebSocket, compatible with JDK 8+).
 *
 * <p>Connects to the central opencli-admin server, registers as an agent,
 * and processes incoming {@code collect} requests by executing opencli locally.</p>

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class OpenCliWsReverseAgentClient implements AutoCloseable {

    private static final JsonMapper MAPPER = new JsonMapper();
    private static final long CLOSE_WAIT_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final OpenCliExecutor localExecutor;

    private final OpenCliWsAgentConnectionProperties connectionProperties;

    private final ExecutorService collectPool;
    private final OpenCliWsCollectRequests collectRequests =
        new OpenCliWsCollectRequests(1024, TimeUnit.MINUTES.toNanos(10), System::nanoTime);

    private volatile boolean shutdownRequested;

    private final AtomicReference<WebSocketClient> activeClient = new AtomicReference<>();

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
        int workers = connectionProperties.getMaxConcurrentCollects();
        int pending = connectionProperties.getMaxPendingCollects();
        if (workers <= 0 || pending < 0 || (long) workers + pending >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Collect capacity requires positive concurrency and a finite pending queue");
        }
        BlockingQueue<Runnable> queue = pending == 0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(pending);
        this.collectPool = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS, queue, runnable -> {
            Thread thread = new Thread(runnable, "opencli-ws-collect");
            thread.setDaemon(true);
            return thread;
        });
        this.localExecutor = new OpenCliExecutor(openCliProperties.copyForLocalCliExecution());
    }

    /**
     * 启动后台重连线程。
     */
    public synchronized void start() {
        if (collectPool.isShutdown()) {
            throw new IllegalStateException("WebSocket worker is closed");
        }
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
        WebSocketClient client = activeClient.getAndSet(null);
        if (Objects.nonNull(client)) {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("ws close failed during stop");
            }
        }
        if (runnerThread != null) {
            runnerThread.interrupt();
        }
    }

    @Override
    public void close() {
        Thread runner;
        synchronized (this) {
            if (collectPool.isShutdown()) {
                return;
            }
            stop();
            collectPool.shutdownNow();
            collectRequests.clear();
            runner = runnerThread;
        }
        long deadline = System.nanoTime() + CLOSE_WAIT_NANOS;
        try {
            collectPool.awaitTermination(CLOSE_WAIT_NANOS, TimeUnit.NANOSECONDS);
            long remaining = deadline - System.nanoTime();
            if (remaining > 0 && Objects.nonNull(runner) && runner != Thread.currentThread()) {
                TimeUnit.NANOSECONDS.timedJoin(runner, remaining);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
                    log.warn("opencli ws session failed attempt={}", attempt);
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
        URI endpoint = URI.create(OpenCliCenterWsUrls.toAgentWebSocketUrl(
            connectionProperties.getCentralApiBaseUrl(), connectionProperties.getWebSocketPath()));
        // The default TLS client verifies the endpoint hostname. Plain WS has session-local identity.
        Object centerIdentity = "wss".equalsIgnoreCase(endpoint.getScheme()) ? endpoint : new Object();

        final CountDownLatch registered = new CountDownLatch(1);
        final CountDownLatch sessionClosed = new CountDownLatch(1);
        final AtomicReference<Throwable> handshakeFailure = new AtomicReference<>();

        log.info("opencli ws connecting");
        WebSocketClient client =
            new WebSocketClient(endpoint) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    if (!isCurrentSession(this)) {
                        closeSession(this);
                        return;
                    }
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
                        handleTextMessage(this, centerIdentity, message, registered, handshakeFailure);
                    } catch (Exception e) {
                        log.warn("handle ws message failed");
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    activeClient.compareAndSet(this, null);
                    if (registered.getCount() > 0) {
                        handshakeFailure.compareAndSet(
                            null,
                            new IllegalStateException(
                                "WebSocket closed before 'registered' (code="
                                    + code
                                    + ")"));
                        registered.countDown();
                    }
                    sessionClosed.countDown();
                }

                @Override
                public void onError(Exception ex) {
                    activeClient.compareAndSet(this, null);
                    log.warn("WebSocket error");
                    if (registered.getCount() > 0) {
                        handshakeFailure.compareAndSet(null, ex);
                        registered.countDown();
                    }
                    sessionClosed.countDown();
                }
            };

        if (!publishSession(client)) {
            return;
        }
        try {
            if (!client.connectBlocking(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("WebSocket connectBlocking returned false");
            }
            if (!registered.await(connectionProperties.getHandshakeTimeoutMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timeout waiting for 'registered' from center");
            }
            Throwable hf = handshakeFailure.get();
            if (Objects.nonNull(hf)) {
                if (hf instanceof Exception) {
                    throw (Exception) hf;
                }
                throw new IllegalStateException(hf);
            }
            sessionClosed.await();
        } finally {
            activeClient.compareAndSet(client, null);
            closeSession(client);
        }
    }

    private synchronized boolean publishSession(WebSocketClient client) {
        if (shutdownRequested) {
            return false;
        }
        activeClient.set(client);
        return true;
    }

    private boolean isCurrentSession(WebSocketClient client) {
        return !shutdownRequested && activeClient.get() == client;
    }

    private static void closeSession(WebSocketClient client) {
        try {
            client.close();
        } catch (Exception e) {
            log.debug("ws session close failed");
        }
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
        if (Objects.nonNull(mode) && !mode.isEmpty() && !"bridge".equals(mode) && !"cdp".equals(mode)) {
            throw new IllegalStateException("mode must be 'bridge' or 'cdp'");
        }
        if (connectionProperties.getWebSocketPath() == OpenCliCenterWebSocketPath.NODES_WS) {
            String nt = connectionProperties.getNodeType();
            if (Objects.nonNull(nt)
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
        reg.put("label", Objects.isNull(connectionProperties.getLabel()) ? "" : connectionProperties.getLabel());
        reg.put(
            "node_type",
            OpenCliStrings.isBlank(connectionProperties.getNodeType())
                ? "shell"
                : connectionProperties.getNodeType().trim());
        return MAPPER.writeValueAsString(reg);
    }

    private void handleTextMessage(
        WebSocketClient ws,
        Object centerIdentity,
        String payload,
        CountDownLatch registered,
        AtomicReference<Throwable> handshakeFailure)
        throws Exception {
        if (!isCurrentSession(ws)) {
            return;
        }
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
            JsonNode id = root.path("request_id");
            String requestId = id.isTextual() ? id.asText() : "";
            if (OpenCliStrings.isBlank(requestId.strip()) || requestId.getBytes(StandardCharsets.UTF_8).length > 128) {
                sendResult(ws, result("", false, Collections.emptyList(), "invalid worker request ID"));
                return;
            }
            if (!isCurrentSession(ws) || !ws.isOpen() || registered.getCount() > 0
                || Objects.nonNull(handshakeFailure.get())) {
                sendResult(ws, result(requestId, false, Collections.emptyList(), "worker is not registered"));
                return;
            }
            admitCollect(ws, centerIdentity, requestId, root);
        }
    }

    private void admitCollect(WebSocketClient ws, Object centerIdentity, String requestId, JsonNode payload)
        throws Exception {
        OpenCliWsCollectRequests.Key key = new OpenCliWsCollectRequests.Key(centerIdentity, requestId);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(MAPPER.writeValueAsBytes(payload));
        OpenCliWsCollectRequests.Admission admission = collectRequests.reserve(key, digest);
        switch (admission.status()) {
            case IN_FLIGHT:
                return;
            case CONFLICT:
                sendResult(ws, result(requestId, false, Collections.emptyList(), "worker request ID conflict"));
                return;
            case CACHED:
                sendResult(ws, admission.cachedResult());
                return;
            case ACCEPTED:
                try {
                    collectPool.execute(() -> runCollectSafe(ws, key, payload));
                } catch (RejectedExecutionException e) {
                    collectRequests.complete(key, null);
                    sendResult(ws, result(requestId, false, Collections.emptyList(), "worker is busy"));
                }
                return;
        }
    }

    private void runCollectSafe(WebSocketClient ws, OpenCliWsCollectRequests.Key key, JsonNode msg) {
        ObjectNode response = null;
        try {
            if (!collectPool.isShutdown() && !Thread.currentThread().isInterrupted()) {
                response = executeCollect(key.requestId(), msg);
            }
        } finally {
            collectRequests.complete(key, response);
        }
        if (Objects.nonNull(response) && isCurrentSession(ws) && ws.isOpen()) {
            sendResult(ws, response);
        }
    }

    private ObjectNode executeCollect(String requestId, JsonNode msg) {
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
                return result(requestId, false, items, "opencli reported failure");
            }
            return result(requestId, true, items, null);
        } catch (OpenCliException e) {
            OpenCliResult partial = e.getPartialResult();
            String err = Objects.nonNull(partial) && OpenCliStrings.isNotBlank(partial.getStderr())
                ? partial.getStderr() : e.getMessage();
            return result(requestId, false, Collections.emptyList(), err);
        } catch (Exception e) {
            log.warn("collect execution failed");
            return result(requestId, false, Collections.emptyList(), "collect execution failed");
        }
    }

    private static Map<String, Object> readArgsMap(JsonNode argsNode) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (Objects.isNull(argsNode) || !argsNode.isObject()) {
            return m;
        }
        for (Map.Entry<String, JsonNode> e : argsNode.properties()) {
            m.put(e.getKey(), readArgValue(e.getValue()));
        }
        return m;
    }

    private static Object readArgValue(JsonNode n) {
        if (Objects.isNull(n) || n.isNull()) {
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
        if (Objects.isNull(node) || !node.isArray()) {
            return list;
        }
        for (JsonNode n : node) {
            if (Objects.nonNull(n) && !n.isNull()) {
                list.add(n.asText());
            }
        }
        return list;
    }

    private static ObjectNode result(String requestId, boolean success, List<JsonNode> items, String error) {
        ObjectNode out = MAPPER.createObjectNode();
        out.put("type", "result");
        out.put("request_id", requestId);
        out.put("success", success);
        ArrayNode arr = out.putArray("items");
        if (Objects.nonNull(items)) {
            items.forEach(arr::add);
        }
        if (Objects.isNull(error) || error.isEmpty()) {
            out.putNull("error");
        } else {
            out.put("error", error);
        }
        return out;
    }

    private static void sendResult(WebSocketClient ws, ObjectNode result) {
        try {
            ws.send(MAPPER.writeValueAsString(result));
        } catch (Exception e) {
            log.error("failed to send WS result");
        }
    }
}
