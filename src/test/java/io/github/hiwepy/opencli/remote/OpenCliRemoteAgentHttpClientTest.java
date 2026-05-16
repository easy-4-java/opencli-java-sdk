package io.github.hiwepy.opencli.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.hiwepy.opencli.OpenCliExecutionTarget;
import io.github.hiwepy.opencli.OpenCliProperties;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.exception.OpenCliNonZeroExitException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 使用 JDK {@link HttpServer} 模拟 Agent {@code POST /collect}，校验 HTTP 客户端与原始报文保留选项。
 */
class OpenCliRemoteAgentHttpClientTest {

    /**
     * 成功响应且开启 capture 时，{@link OpenCliResult#getRemoteRawHttpBody()} 与响应体一致。
     */
    @Test
    void collectSuccessCapturesRawBodyWhenEnabled() throws IOException {
        String json = "{\"success\":true,\"items\":[{\"k\":\"v\"}],\"error\":\"\"}";
        try (JsonCollectStub stub = JsonCollectStub.start(json)) {
            OpenCliProperties props = baseProps(stub.port());
            props.setRemoteCaptureRawHttpResponse(true);
            OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props);
            OpenCliCollectRequest req =
                OpenCliCollectRequest.builder()
                    .site("npm")
                    .command("package")
                    .build();
            OpenCliResult r = client.collect(req);
            Assertions.assertTrue(r.isSuccess());
            Assertions.assertEquals(json, r.getRemoteRawHttpBody());
        }
    }

    /**
     * 未开启 capture 时 {@link OpenCliResult#getRemoteRawHttpBody()} 应为 null。
     */
    @Test
    void collectSuccessOmitsRawBodyWhenDisabled() throws IOException {
        String json = "{\"success\":true,\"items\":[],\"error\":\"\"}";
        try (JsonCollectStub stub = JsonCollectStub.start(json)) {
            OpenCliProperties props = baseProps(stub.port());
            props.setRemoteCaptureRawHttpResponse(false);
            OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props);
            OpenCliResult r =
                client.collect(
                    OpenCliCollectRequest.builder().site("npm").command("package").build());
            Assertions.assertTrue(r.isSuccess());
            Assertions.assertNull(r.getRemoteRawHttpBody());
        }
    }

    /**
     * Agent 返回 success=false 时抛出 {@link OpenCliNonZeroExitException}，失败快照仍可带原文。
     */
    @Test
    void collectFailureIncludesRawBodyWhenEnabled() throws IOException {
        String json = "{\"success\":false,\"items\":null,\"error\":\"agent-error\"}";
        try (JsonCollectStub stub = JsonCollectStub.start(json)) {
            OpenCliProperties props = baseProps(stub.port());
            props.setRemoteCaptureRawHttpResponse(true);
            OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props);
            OpenCliNonZeroExitException ex =
                Assertions.assertThrows(
                    OpenCliNonZeroExitException.class,
                    () ->
                        client.collect(
                            OpenCliCollectRequest.builder().site("x").command("y").build()));
            Assertions.assertNotNull(ex.getPartialResult());
            Assertions.assertEquals(json, ex.getPartialResult().getRemoteRawHttpBody());
        }
    }

    private static OpenCliProperties baseProps(int port) {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        props.setRemoteAgentBaseUrl("http://127.0.0.1:" + port);
        props.setCommandTimeoutMillis(5_000L);
        return props;
    }

    /**
     * 本地 {@code POST /collect} 桩，可 try-with-resources 关闭。
     */
    private static final class JsonCollectStub implements AutoCloseable {

        private final HttpServer server;

        private final int port;

        private JsonCollectStub(HttpServer server, int port) {
            this.server = server;
            this.port = port;
        }

        /**
         * 启动仅处理 {@code POST /collect} 的服务并返回 UTF-8 JSON 固定响应。
         *
         * @param jsonBody 响应正文
         * @return 可关闭的桩对象
         */
        static JsonCollectStub start(String jsonBody) throws IOException {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext(
                "/collect",
                new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                            exchange.sendResponseHeaders(405, -1);
                            exchange.close();
                            return;
                        }
                        exchange.getRequestBody().readAllBytes();
                        exchange
                            .getResponseHeaders()
                            .set("Content-Type", "application/json; charset=utf-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    }
                });
            server.start();
            int port = server.getAddress().getPort();
            return new JsonCollectStub(server, port);
        }

        int port() {
            return port;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
