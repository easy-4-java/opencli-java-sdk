package io.github.easy4j.opencli.center.ws;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliCancellationToken;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
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

    private final ThreadPoolExecutor collectPool;
    private final Set<CollectTask> collectTasks = new HashSet<>();
    private final OpenCliWsCollectRequests collectRequests =
        new OpenCliWsCollectRequests(1024, TimeUnit.MINUTES.toNanos(10), System::nanoTime);

    private volatile boolean shutdownRequested;

    private final AtomicReference<WebSocketClient> activeClient = new AtomicReference<>();

    private Thread runnerThread;
    private boolean stopping;
    private long admissionEpoch;

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
        if (stopping || (Objects.nonNull(runnerThread) && runnerThread.isAlive())) {
            return;
        }
        shutdownRequested = false;
        runnerThread = new Thread(this::runLoop, "opencli-ws-agent-runner");
        runnerThread.setDaemon(true);
        runnerThread.start();
    }

    /**
     * 停止接单并丢弃未开始的任务。在有限时间内等待已在途任务，随后请求取消。
     * 停止完成后可重新启动同一实例。
     */
    public void stop() {
        long deadline = System.nanoTime() + CLOSE_WAIT_NANOS;
        WebSocketClient client;
        Thread runner;
        synchronized (this) {
            if (shutdownRequested || collectPool.isShutdown()) {
                return;
            }
            stopping = true;
            client = stopAdmission();
            runner = runnerThread;
        }
        try {
            if (Objects.nonNull(client)) {
                closeSession(client);
            }
            synchronized (this) {
                long remaining = deadline - System.nanoTime();
                while (!collectTasks.isEmpty() && remaining > 0) {
                    TimeUnit.NANOSECONDS.timedWait(this, remaining);
                    remaining = deadline - System.nanoTime();
                }
            }
            awaitRunner(runner, deadline);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (this) {
                cancelOutstandingCollects();
                stopping = false;
            }
        }
    }

    @Override
    public void close() {
        long deadline = System.nanoTime() + CLOSE_WAIT_NANOS;
        WebSocketClient client;
        Thread runner;
        synchronized (this) {
            if (collectPool.isShutdown()) {
                return;
            }
            client = stopAdmission();
            cancelOutstandingCollects();
            collectPool.shutdownNow();
            collectRequests.clear();
            runner = runnerThread;
        }
        if (Objects.nonNull(client)) {
            closeSession(client);
        }
        try {
            long remaining = deadline - System.nanoTime();
            if (remaining > 0) {
                collectPool.awaitTermination(remaining, TimeUnit.NANOSECONDS);
            }
            awaitRunner(runner, deadline);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // The caller holds this worker's monitor while invalidating admission and the queue.
    private WebSocketClient stopAdmission() {
        shutdownRequested = true;
        admissionEpoch++;
        WebSocketClient client = activeClient.getAndSet(null);
        List<Runnable> pending = new ArrayList<>();
        collectPool.getQueue().drainTo(pending);
        for (Runnable waiting : pending) {
            finishCollect((CollectTask) waiting, null);
        }
        if (Objects.nonNull(runnerThread)) {
            runnerThread.interrupt();
        }
        return client;
    }

    private void cancelOutstandingCollects() {
        for (CollectTask task : collectTasks) {
            task.cancellation.cancel();
        }
    }

    private static void awaitRunner(Thread runner, long deadline) throws InterruptedException {
        long remaining = deadline - System.nanoTime();
        if (remaining > 0 && Objects.nonNull(runner) && runner != Thread.currentThread()) {
            TimeUnit.NANOSECONDS.timedJoin(runner, remaining);
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
        OpenCliWsCollectRequests.Admission admission;
        boolean busy = false;
        synchronized (this) {
            if (!isCurrentSession(ws)) {
                return;
            }
            admission = collectRequests.reserve(key, digest);
            if (admission.status() == OpenCliWsCollectRequests.Status.ACCEPTED) {
                CollectTask task = new CollectTask(ws, key, payload, admissionEpoch);
                collectTasks.add(task);
                try {
                    collectPool.execute(task);
                } catch (RejectedExecutionException e) {
                    finishCollect(task, null);
                    busy = true;
                }
            }
        }
        if (!isCurrentSession(ws)) {
            return;
        }
        if (busy) {
            sendResult(ws, result(requestId, false, Collections.emptyList(), "worker is busy"));
        } else if (admission.status() == OpenCliWsCollectRequests.Status.CONFLICT) {
            sendResult(ws, result(requestId, false, Collections.emptyList(), "worker request ID conflict"));
        } else if (admission.status() == OpenCliWsCollectRequests.Status.CACHED) {
            sendResult(ws, admission.cachedResult());
        }
    }

    private synchronized void finishCollect(CollectTask task, ObjectNode response) {
        collectRequests.complete(task.key, response);
        collectTasks.remove(task);
        notifyAll();
    }

    private synchronized boolean mayStartCollect(long epoch) {
        return !shutdownRequested && epoch == admissionEpoch && !Thread.currentThread().isInterrupted();
    }

    private final class CollectTask implements Runnable {
        private final WebSocketClient socket;
        private final OpenCliWsCollectRequests.Key key;
        private final JsonNode message;
        private final long epoch;
        private final OpenCliCancellationToken cancellation = new OpenCliCancellationToken();

        CollectTask(WebSocketClient socket, OpenCliWsCollectRequests.Key key, JsonNode message, long epoch) {
            this.socket = socket;
            this.key = key;
            this.message = message;
            this.epoch = epoch;
        }

        @Override
        public void run() {
            ObjectNode response = null;
            try {
                if (mayStartCollect(epoch)) {
                    response = executeCollect(key.requestId(), message, cancellation);
                }
            } finally {
                finishCollect(this, response);
            }
            if (Objects.nonNull(response) && isCurrentSession(socket) && socket.isOpen()) {
                sendResult(socket, response);
            }
        }
    }

    private ObjectNode executeCollect(String requestId, JsonNode msg, OpenCliCancellationToken cancellation) {
        try {
            String site = msg.path("site").asText("");
            String command = msg.path("command").asText("");
            String format = msg.path("format").asText("json");
            Map<String, Object> args = readArgsMap(msg.get("args"));
            List<String> positional = readPositionalList(msg.get("positional_args"));

            List<String> argv =
                OpenCliCenterWsCollectToArgv.toArgv(site, command, positional, args, format);
            OpenCliResult r = localExecutor.invoke(argv, cancellation);
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
