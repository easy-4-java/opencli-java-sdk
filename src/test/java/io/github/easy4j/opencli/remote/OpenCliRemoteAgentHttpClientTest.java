package io.github.easy4j.opencli.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliRemoteProtocolException;
import io.github.easy4j.opencli.meta.OpenCliExternalClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void nonSuccessHttpResponseDoesNotExposeBodyCanaryInException() throws IOException {
        String canary = "body-secret-" + UUID.randomUUID();
        String urlCanary = "url-secret-" + UUID.randomUUID();
        try (JsonCollectStub stub = JsonCollectStub.start(canary, 503)) {
            OpenCliProperties props = baseProps(stub.port());
            props.setRemoteAgentBaseUrl("http://127.0.0.1:" + stub.port()
                + "/collect?token=" + urlCanary);
            OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props);
            ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
            PrintStream previousErr = System.err;
            OpenCliExecutableFailureException ex;
            try (PrintStream capture = new PrintStream(diagnostics, true, "UTF-8")) {
                System.setErr(capture);
                try {
                    ex = Assertions.assertThrows(OpenCliExecutableFailureException.class,
                        () -> client.collect(OpenCliCollectRequest.builder().site("x").command("y").build()));
                } finally {
                    System.setErr(previousErr);
                }
            }
            Assertions.assertTrue(ex.getMessage().contains("503"));
            Assertions.assertFalse(ex.toString().contains(canary));
            Assertions.assertFalse(ex.toString().contains(urlCanary));
            String logText = diagnostics.toString(StandardCharsets.UTF_8.name());
            Assertions.assertTrue(logText.contains("Agent HTTP non-2xx"));
            Matcher correlation = Pattern.compile("correlationId=([0-9a-f-]{36})")
                .matcher(ex.getMessage());
            Assertions.assertTrue(correlation.find());
            Assertions.assertDoesNotThrow(() -> UUID.fromString(correlation.group(1)));
            Assertions.assertTrue(logText.contains(correlation.group(0)));
            Assertions.assertFalse(logText.contains(canary));
            Assertions.assertFalse(logText.contains(urlCanary));
            for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
                Assertions.assertFalse(cause.toString().contains(canary));
                Assertions.assertFalse(cause.toString().contains(urlCanary));
            }
        }
    }

    @Test
    void malformedHttpEndpointDoesNotExposeCredentialsInPublicStackTrace() {
        String credential = "private-" + UUID.randomUUID();
        OpenCliProperties properties = baseProps(1);
        properties.setRemoteAgentBaseUrl("http://operator:" + credential + "@invalid host");
        try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(properties)) {
            OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("x").command("y").build();
            OpenCliExecutableFailureException failure = Assertions.assertThrows(
                OpenCliExecutableFailureException.class, () -> client.collect(request));
            StringWriter stack = new StringWriter();
            failure.printStackTrace(new PrintWriter(stack));
            Assertions.assertFalse(stack.toString().contains(credential));
            Assertions.assertTrue(failure.getMessage().contains("correlationId="));
        }
    }

    @Test
    void nullEnvelopeUsesSafeCorrelatedParseFailure() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start("null");
             OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(stub.port()))) {
            OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("x").command("y").build();
            OpenCliExecutableFailureException failure = Assertions.assertThrows(
                OpenCliExecutableFailureException.class, () -> client.collect(request));
            Assertions.assertTrue(failure.getMessage().contains("response envelope"));
            Assertions.assertTrue(failure.getMessage().contains("correlationId="));
            Assertions.assertNull(failure.getCause());
        }
    }

    @Test
    void losslessAdapterRequestReachesLegacyEndpoint() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true}");
             OpenCliExecutor executor = new OpenCliExecutor(baseProps(stub.port()))) {
            OpenCliResult result = executor.invoke("npm", "search", "react components", "--limit", "5");
            Assertions.assertTrue(result.isSuccess());
            Assertions.assertEquals(1, stub.requests());
            Assertions.assertTrue(stub.requestBody.contains("\"site\":\"npm\""));
            Assertions.assertTrue(stub.requestBody.contains("\"command\":\"search\""));
            Assertions.assertTrue(stub.requestBody.contains("\"positional_args\":[\"react components\"]"));
            Assertions.assertTrue(stub.requestBody.contains("\"args\":{\"limit\":\"5\"}"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "browser|session-a|state", "list|-f|json", "external|list", "unverified-tool|version",
        "npm|search|--tag|one|--tag=two", "npm|search|--|react", "npm|search|--limit|-1",
        "npm|search|--limit=-1", "npm|search|", "npm|search|--query=", "npm|search|--query| padded ",
        "npm|search|-f|json|--format=yaml", "npm|search|--format|--verbose",
        "npm|search|-v", "npm|search|--limit|5|react", " npm|search"
    })
    void unrepresentableArgvIsRejectedBeforeHttp(String input) throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true}");
             OpenCliExecutor executor = new OpenCliExecutor(baseProps(stub.port()))) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> executor.invoke(input.split("\\|", -1)));
            Assertions.assertEquals(0, stub.requests());
        }
    }

    @Test
    void externalPassthroughCannotBeRoutedAsAnAdapter() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true}");
             OpenCliExecutor executor = new OpenCliExecutor(baseProps(stub.port()))) {
            OpenCliExternalClient external = new OpenCliExternalClient(executor);
            Assertions.assertThrows(UnsupportedOperationException.class,
                () -> external.passthrough("npm", Arrays.asList("search", "react")));
            Assertions.assertEquals(0, stub.requests());
        }
    }

    @Test
    void explicitSuccessfulReplyCanOmitItems() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true,\"extension\":42}");
             OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(stub.port()))) {
            OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("x").command("y").build();
            OpenCliResult result = client.collect(request);
            Assertions.assertTrue(result.isSuccess());
            Assertions.assertEquals("[]", result.getStdout());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "null",
        "[]",
        "{\"items\":[]}",
        "{\"success\":null}",
        "{\"success\":\"true\"}",
        "{\"success\":1}",
        "{\"success\":true,\"error\":42}",
        "{\"success\":false,\"success\":true}",
        "{\"success\":true} {\"success\":false}"
    })
    void invalidLegacyEnvelopeIsAProtocolFailure(String body) throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start(body);
             OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(stub.port()))) {
            OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("x").command("y").build();
            OpenCliRemoteProtocolException failure = Assertions.assertThrows(
                OpenCliRemoteProtocolException.class, () -> client.collect(request));
            Assertions.assertTrue(failure.getMessage().contains("response envelope"));
            Assertions.assertTrue(failure.getMessage().contains("correlationId="));
            Assertions.assertNull(failure.getCause());
        }
    }

    @Test
    void malformedResponseDoesNotExposeParserSnippetInException() throws IOException {
        String canary = "parser-secret-" + UUID.randomUUID();
        try (JsonCollectStub stub = JsonCollectStub.start("{\"error\":\"" + canary)) {
            OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(stub.port()));
            OpenCliExecutableFailureException ex = Assertions.assertThrows(
                OpenCliExecutableFailureException.class,
                () -> client.collect(OpenCliCollectRequest.builder().site("x").command("y").build()));
            Assertions.assertFalse(ex.toString().contains(canary));
            for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
                Assertions.assertFalse(cause.toString().contains(canary));
            }
        }
    }

    @Test
    void remoteFailureKeepsRawResultWithoutExposingErrorCanaryInException() throws IOException {
        String canary = "agent-secret-" + UUID.randomUUID();
        String json = "{\"success\":false,\"items\":null,\"error\":\"" + canary + "\"}";
        try (JsonCollectStub stub = JsonCollectStub.start(json)) {
            OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(stub.port()));
            OpenCliNonZeroExitException ex = Assertions.assertThrows(
                OpenCliNonZeroExitException.class,
                () -> client.collect(OpenCliCollectRequest.builder().site("x").command("y").build()));
            Assertions.assertFalse(ex.toString().contains(canary));
            Assertions.assertEquals(canary, ex.getPartialResult().getStderr());
            Assertions.assertNull(ex.getPartialResult().getRemoteRawHttpBody());
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
        private final AtomicInteger requestCount = new AtomicInteger();
        private volatile String requestBody;

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
            return start(jsonBody, 200);
        }

        static JsonCollectStub start(String jsonBody, int status) throws IOException {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            JsonCollectStub stub = new JsonCollectStub(server, server.getAddress().getPort());
            server.createContext(
                "/collect",
                new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        stub.requestCount.incrementAndGet();
                        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                            exchange.sendResponseHeaders(405, -1);
                            exchange.close();
                            return;
                        }
                        try (Scanner request = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8.name())) {
                            request.useDelimiter("\\A");
                            stub.requestBody = request.hasNext() ? request.next() : "";
                        }
                        exchange
                            .getResponseHeaders()
                            .set("Content-Type", "application/json; charset=utf-8");
                        exchange.sendResponseHeaders(status, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    }
                });
            server.start();
            return stub;
        }

        int port() {
            return port;
        }

        int requests() {
            return requestCount.get();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    /**
     * close 幂等；close 之后 collect 抛 {@link IllegalStateException}。
     */
    @Test
    void closeIsIdempotentAndRejectsLaterCollect() {
        OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(
            new io.github.easy4j.opencli.OpenCliProperties());
        client.close();
        client.close();
        Assertions.assertThrows(IllegalStateException.class,
            () -> client.collect(new OpenCliCollectRequest()));
    }
}
