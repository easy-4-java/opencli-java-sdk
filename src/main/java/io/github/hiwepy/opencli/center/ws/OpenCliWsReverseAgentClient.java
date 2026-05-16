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
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 边缘侧反向 WebSocket 客户端：与 opencli-admin 中心建立长连接，协议对齐
 * {@code backend/ws_agent_manager.py}（中心下发 {@code collect}）与
 * {@code backend/agent_server.py}（边缘处理并回复 {@code result}）。
 * <p>
 * 本类在收到 {@code collect} 时<strong>仅调用本机</strong> {@link OpenCliExecutor}（强制本地子进程），
 * 即扮演 Python Agent 容器中「在边缘执行 opencli」的角色；不实现中心侧的 {@code dispatch_collect}。
 * </p>
 * <p>
 * 在后台线程中运行会话与自动重连（指数退避上界与 Python 类似）。调用 {@link #start()} /
 * {@link #stop()} 控制生命周期。
 * </p>
 */
@Slf4j
public final class OpenCliWsReverseAgentClient implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OpenCliExecutor localExecutor;

    private final OpenCliWsAgentConnectionProperties connectionProperties;

    private final HttpClient httpClient;

    private final ExecutorService collectPool =
        Executors.newCachedThreadPool(
            r -> {
                Thread t = new Thread(r, "opencli-ws-collect");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean shutdownRequested;

    private volatile WebSocket activeWebSocket;

    private Thread runnerThread;

    /**
     * @param openCliProperties  用于本地 opencli 的完整配置；内部会 {@link OpenCliProperties#copyForLocalCliExecution()}，忽略远程执行目标
     * @param connectionProperties 与中心连接、注册相关参数
     */
    public OpenCliWsReverseAgentClient(
        OpenCliProperties openCliProperties, OpenCliWsAgentConnectionProperties connectionProperties) {
        Objects.requireNonNull(openCliProperties, "openCliProperties");
        this.connectionProperties =
            Objects.requireNonNull(connectionProperties, "connectionProperties");
        this.localExecutor = new OpenCliExecutor(openCliProperties.copyForLocalCliExecution());
        this.httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    /**
     * 启动后台线程：连接中心、注册、处理 {@code collect} / ping；断线后自动重连直至 {@link #stop()}。
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
     * 请求停止：关闭 WebSocket 并结束重连循环；不阻塞等待线程退出。
     */
    public synchronized void stop() {
        shutdownRequested = true;
        WebSocket ws = activeWebSocket;
        activeWebSocket = null;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "client stop");
            } catch (Exception e) {
                log.debug("sendClose ignored: {}", e.getMessage());
            }
            try {
                ws.abort();
            } catch (Exception e) {
                log.debug("abort ignored: {}", e.getMessage());
            }
        }
        if (runnerThread != null) {
            runnerThread.interrupt();
        }
    }

    /**
     * 等价于 {@link #stop()}，并关闭 collect 线程池。
     */
    @Override
    public void close() {
        stop();
        collectPool.shutdown();
    }

    /**
     * 重连主循环：单次会话阻塞直至连接关闭，再根据配置休眠后重试。
     */
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

    /**
     * 建立一条 WebSocket：发送 register、等待 registered，再阻塞直到连接关闭。
     */
    private void runOneSession() throws Exception {
        validateConnectionProperties();
        String wsUrl =
            OpenCliCenterWsUrls.toAgentWebSocketUrl(
                connectionProperties.getCentralApiBaseUrl(), connectionProperties.getWebSocketPath());

        CompletableFuture<Void> registrationAck = new CompletableFuture<>();
        CompletableFuture<WebSocket> buildOk = new CompletableFuture<>();
        CompletableFuture<Void> sessionDone = new CompletableFuture<>();

        WebSocket.Listener listener =
            new AgentListener(
                registrationAck,
                sessionDone,
                this.localExecutor,
                this.collectPool);

        log.info("opencli ws connecting: {}", wsUrl);
        httpClient
            .newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), listener)
            .whenComplete(
                (ws, ex) -> {
                    if (ex != null) {
                        buildOk.completeExceptionally(ex);
                    } else {
                        activeWebSocket = ws;
                        buildOk.complete(ws);
                    }
                });

        WebSocket ws = buildOk.get(30, TimeUnit.SECONDS);
        sendRegisterMessage(ws);

        try {
            registrationAck.get(connectionProperties.getHandshakeTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "handshake failed");
            } catch (Exception ignored) {
            }
            throw new IllegalStateException(
                "Handshake failed (expected 'registered' from center): " + e.getMessage(), e);
        }

        try {
            sessionDone.get();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new IllegalStateException(cause);
        } finally {
            activeWebSocket = null;
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

    /**
     * 发送与 Python {@code agent_server} 一致的 register JSON（含 {@code node_type}）。
     */
    private void sendRegisterMessage(WebSocket ws) {
        try {
            ObjectNode reg = MAPPER.createObjectNode();
            reg.put("type", "register");
            reg.put("agent_url", connectionProperties.getAgentAdvertiseUrl().trim());
            reg.put("mode", OpenCliStrings.isBlank(connectionProperties.getMode()) ? "cdp" : connectionProperties.getMode().trim());
            reg.put("label", connectionProperties.getLabel() == null ? "" : connectionProperties.getLabel());
            reg.put("node_type", OpenCliStrings.isBlank(connectionProperties.getNodeType()) ? "shell"
                : connectionProperties.getNodeType().trim());
            String json = MAPPER.writeValueAsString(reg);
            ws.sendText(json, true).join();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send register: " + e.getMessage(), e);
        }
    }

    /**
     * WebSocket 回调：文本拼帧、register 握手、ping/pong、分发 collect。
     */
    private static final class AgentListener implements WebSocket.Listener {

        private final StringBuilder textBuffer = new StringBuilder();

        private final CompletableFuture<Void> registrationAck;

        private final CompletableFuture<Void> sessionDone;

        private final OpenCliExecutor localExecutor;

        private final ExecutorService collectPool;

        AgentListener(
            CompletableFuture<Void> registrationAck,
            CompletableFuture<Void> sessionDone,
            OpenCliExecutor localExecutor,
            ExecutorService collectPool) {
            this.registrationAck = registrationAck;
            this.sessionDone = sessionDone;
            this.localExecutor = localExecutor;
            this.collectPool = collectPool;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (!last) {
                webSocket.request(1);
                return null;
            }
            String payload = textBuffer.toString();
            textBuffer.setLength(0);
            try {
                handleCompleteText(webSocket, payload);
            } catch (Exception e) {
                log.warn("handle ws text failed: {}", e.getMessage());
            }
            webSocket.request(1);
            return null;
        }

        private void handleCompleteText(WebSocket ws, String payload) throws Exception {
            JsonNode root = MAPPER.readTree(payload);
            String type = root.path("type").asText("");
            if ("registered".equals(type)) {
                registrationAck.complete(null);
                return;
            }
            if ("ping".equals(type)) {
                ws.sendText("{\"type\":\"pong\"}", true).join();
                return;
            }
            if ("pong".equals(type)) {
                return;
            }
            if ("collect".equals(type)) {
                collectPool.submit(() -> runCollectSafe(ws, root));
            }
        }

        /**
         * 执行 collect 并发送 result；单独捕获异常，避免线程池静默吞掉错误。
         */
        private void runCollectSafe(WebSocket ws, JsonNode msg) {
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
                List<JsonNode> items =
                    OpenCliWsCollectStdoutItems.parseItems(r.getStdout(), format);
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
                sendResult(ws, requestId, false, List.of(), err);
            } catch (OpenCliException e) {
                OpenCliResult partial = e.getPartialResult();
                String err =
                    partial != null && OpenCliStrings.isNotBlank(partial.getStderr())
                        ? partial.getStderr()
                        : e.getMessage();
                sendResult(ws, requestId, false, List.of(), err);
            } catch (Exception e) {
                log.warn("collect execution error request_id={}: {}", requestId, e.getMessage());
                sendResult(ws, requestId, false, List.of(), e.getMessage());
            }
        }

        private static Map<String, Object> readArgsMap(JsonNode argsNode) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (argsNode == null || !argsNode.isObject()) {
                return m;
            }
            argsNode.fields().forEachRemaining(e -> m.put(e.getKey(), readArgValue(e.getValue())));
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
            WebSocket ws, String requestId, boolean success, List<JsonNode> items, String error) {
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
                ws.sendText(MAPPER.writeValueAsString(out), true).join();
            } catch (Exception e) {
                log.error("failed to send WS result request_id={}: {}", requestId, e.getMessage());
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (!registrationAck.isDone()) {
                registrationAck.completeExceptionally(
                    new IllegalStateException(
                        "WebSocket closed before 'registered' (code=" + statusCode + " reason=" + reason + ")"));
            }
            if (!sessionDone.isDone()) {
                sessionDone.complete(null);
            }
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("WebSocket error: {}", error.toString());
            if (!registrationAck.isDone()) {
                registrationAck.completeExceptionally(error);
            }
            if (!sessionDone.isDone()) {
                sessionDone.completeExceptionally(error);
            }
        }
    }
}
