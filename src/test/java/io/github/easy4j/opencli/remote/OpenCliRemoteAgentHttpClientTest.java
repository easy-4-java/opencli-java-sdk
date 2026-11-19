package io.github.easy4j.opencli.remote;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.OpenCliRemoteAuthentication;
import io.github.easy4j.opencli.core.OpenCliDiagnosticExport;
import io.github.easy4j.opencli.core.OpenCliRemoteExecutionDetails;
import io.github.easy4j.opencli.core.OpenCliRemoteExecutionDetails.ExitCodeSource;
import io.github.easy4j.opencli.core.OpenCliRemoteExecutionDetails.ItemsSource;
import io.github.easy4j.opencli.core.OpenCliRemoteExecutionDetails.Outcome;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliRemoteProtocolException;
import io.github.easy4j.opencli.exception.OpenCliRemoteResponseLimitException;
import io.github.easy4j.opencli.exception.OpenCliRemoteTimeoutException;
import io.github.easy4j.opencli.meta.OpenCliExternalClient;
import io.github.easy4j.opencli.testsupport.OpenCliTlsFixture;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
            OpenCliResult result = ex.getPartialResult();
            OpenCliRemoteExecutionDetails details = result.getRemoteExecutionDetails();
            Assertions.assertNotNull(details);
            Assertions.assertEquals(200, details.getHttpStatus());
            Assertions.assertEquals(Boolean.FALSE, details.getLegacySuccess());
            Assertions.assertEquals(Outcome.COMMAND_FAILURE, details.getOutcome());
            Assertions.assertEquals(ItemsSource.NULL_DEFAULT, details.getItemsSource());
            Assertions.assertEquals(Integer.valueOf(1), result.getExitCode());
            Assertions.assertEquals(ExitCodeSource.SYNTHETIC_COMPATIBILITY, details.getExitCodeSource());
            Assertions.assertFalse(details.getObservedRemoteExitCode().isPresent());
            Assertions.assertNull(result.getExecutionDetails());
            String exported = OpenCliDiagnosticExport.export(result, 250);
            Assertions.assertTrue(exported.contains("exitCodeSource=SYNTHETIC_COMPATIBILITY"));
            Assertions.assertFalse(exported.contains("agent-error"));
        }
    }

    @Test
    void totalDeadlineStopsAnActiveChunkedResponseAndDoesNotRetry() throws IOException {
        AtomicInteger writes = new AtomicInteger();
        AtomicInteger requests = new AtomicInteger();
        try (JsonCollectStub stub = JsonCollectStub.start(exchange -> {
            if (requests.incrementAndGet() > 1) {
                byte[] reply = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, reply.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(reply);
                }
                return;
            }
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                for (int index = 0; index < 50; index++) {
                    output.write(' ');
                    output.flush();
                    writes.incrementAndGet();
                    try {
                        Thread.sleep(125);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        })) {
            OpenCliProperties props = baseProps(stub.port());
            props.setCommandTimeoutMillis(900);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                OpenCliRemoteTimeoutException timeout = Assertions.assertTimeoutPreemptively(
                    Duration.ofSeconds(3), () -> Assertions.assertThrows(
                        OpenCliRemoteTimeoutException.class, () -> client.collect(request)));
                Assertions.assertEquals(900, timeout.getTimeoutMillis());
                Assertions.assertTrue(timeout.isRemoteOutcomeUnknown());
                Assertions.assertNull(timeout.getCause());
                Assertions.assertTrue(writes.get() > 1);
                Assertions.assertNull(timeout.getPartialResult());
                Assertions.assertEquals(1, stub.requests());
                props.setCommandTimeoutMillis(4_000);
                Assertions.assertTrue(client.collect(request).isSuccess());
                Assertions.assertEquals(2, stub.requests());
            }
        }
    }

    @Test
    void refusesAnExpiredInvocationBudgetBeforeDispatch() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true}")) {
            OpenCliProperties props = baseProps(stub.port());
            props.setCommandTimeoutMillis(100);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                long submittedAt = System.nanoTime() - TimeUnit.SECONDS.toNanos(1);
                OpenCliRemoteTimeoutException timeout = Assertions.assertThrows(
                    OpenCliRemoteTimeoutException.class, () -> client.collect(request, submittedAt));
                Assertions.assertFalse(timeout.isRemoteOutcomeUnknown());
                Assertions.assertEquals(0, stub.requests());
            }
        }
    }

    @Test
    void doesNotReplayCollectAtARedirectLocation() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start(exchange -> {
            exchange.getResponseHeaders().set("Location", "/collect?retry=true");
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
        });
             OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(stub.port()))) {
            OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
            OpenCliExecutableFailureException failure = Assertions.assertThrows(
                OpenCliExecutableFailureException.class, () -> client.collect(request));
            Assertions.assertTrue(failure.getMessage().contains("307"));
            Assertions.assertEquals(1, stub.requests());
        }
    }

    @Test
    void permitsABodyAtTheByteLimitAndRejectsOneByteLess() throws IOException {
        String body = "{\"success\":true,\"items\":[\"你好\"]}";
        try (JsonCollectStub stub = JsonCollectStub.start(body)) {
            OpenCliProperties props = baseProps(stub.port());
            props.setMaxRemoteResponseBytes(body.getBytes(StandardCharsets.UTF_8).length);
            props.setRemoteCaptureRawHttpResponse(true);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                Assertions.assertEquals(body, client.collect(request).getRemoteRawHttpBody());
                props.setMaxRemoteResponseBytes(props.getMaxRemoteResponseBytes() - 1);
                OpenCliRemoteResponseLimitException exception = Assertions.assertThrows(
                    OpenCliRemoteResponseLimitException.class, () -> client.collect(request));
                Assertions.assertEquals(200, exception.getHttpStatus());
                Assertions.assertEquals(props.getMaxRemoteResponseBytes(), exception.getMaxResponseBytes());
                Assertions.assertNull(exception.getPartialResult());
                props.setMaxRemoteResponseBytes(props.getMaxRemoteResponseBytes() + 1);
                Assertions.assertEquals(body, client.collect(request).getRemoteRawHttpBody());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 503})
    void stopsReadingOversizedChunkedBodiesWithoutWaitingForEof(int status) throws IOException {
        CountDownLatch release = new CountDownLatch(1);
        byte[] prefix = ("{\"success\":true,\"items\":[\"private-body-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        try (JsonCollectStub stub = JsonCollectStub.start(exchange -> {
            exchange.sendResponseHeaders(status, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(prefix);
                output.flush();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        })) {
            try {
                OpenCliProperties props = baseProps(stub.port());
                props.setMaxRemoteResponseBytes(24);
                props.setCommandTimeoutMillis(8_000);
                props.setRemoteCaptureRawHttpResponse(true);
                try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                    OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                    OpenCliRemoteResponseLimitException exception = Assertions.assertTimeoutPreemptively(
                        Duration.ofSeconds(3), () -> Assertions.assertThrows(
                            OpenCliRemoteResponseLimitException.class, () -> client.collect(request)));
                    Assertions.assertEquals(status, exception.getHttpStatus());
                    Assertions.assertEquals(24, exception.getMaxResponseBytes());
                    Assertions.assertFalse(exception.getMessage().contains("private-body-"));
                    Assertions.assertNull(exception.getCause());
                    Assertions.assertNull(exception.getPartialResult());
                    Assertions.assertEquals(1, stub.requests());
                }
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void capsDecodedGzipBodyRatherThanCompressedContentLength() throws IOException {
        StringBuilder body = new StringBuilder("{\"success\":true,\"items\":[\"");
        for (int i = 0; i < 500; i++) {
            body.append('a');
        }
        body.append("\"]}");
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        Assertions.assertTrue(compressed.size() < 128);
        try (JsonCollectStub stub = JsonCollectStub.start(exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, compressed.size());
            try (OutputStream output = exchange.getResponseBody()) {
                compressed.writeTo(output);
            }
        })) {
            OpenCliProperties props = baseProps(stub.port());
            props.setMaxRemoteResponseBytes(128);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                OpenCliRemoteResponseLimitException exception = Assertions.assertThrows(
                    OpenCliRemoteResponseLimitException.class, () -> client.collect(request));
                Assertions.assertTrue(exception.getMessage().contains("response body exceeded"));
            }
        }
    }

    @Test
    void rejectsNonpositiveResponseLimitBeforeHttp() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true}")) {
            OpenCliProperties props = baseProps(stub.port());
            props.setMaxRemoteResponseBytes(0);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                Assertions.assertThrows(IllegalArgumentException.class, () -> client.collect(request));
                Assertions.assertEquals(0, stub.requests());
            }
        }
    }

    @Test
    void keepsDeclaredCharsetWithinTheResponseLimit() throws IOException {
        String body = "{\"success\":true,\"items\":[\"café\"]}";
        byte[] bytes = body.getBytes(StandardCharsets.ISO_8859_1);
        try (JsonCollectStub stub = JsonCollectStub.start(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=ISO-8859-1");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        })) {
            OpenCliProperties props = baseProps(stub.port());
            props.setMaxRemoteResponseBytes(bytes.length);
            props.setRemoteCaptureRawHttpResponse(true);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                Assertions.assertEquals(body, client.collect(request).getRemoteRawHttpBody());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 503})
    void invalidResponseCharsetRetainsTheObservedHttpStatus(int status) throws IOException {
        String canary = "unexpected-charset-body";
        try (JsonCollectStub stub = JsonCollectStub.start(exchange -> {
            byte[] body = canary.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=no-such-charset");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        })) {
            OpenCliProperties props = baseProps(stub.port());
            props.setRemoteCaptureRawHttpResponse(true);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliExecutableFailureException failure = Assertions.assertThrows(
                    OpenCliExecutableFailureException.class,
                    () -> client.collect(OpenCliCollectRequest.builder().site("x").command("y").build()));
                Assertions.assertEquals(status == 200 ? OpenCliRemoteProtocolException.class
                    : OpenCliExecutableFailureException.class, failure.getClass());
                OpenCliResult partial = failure.getPartialResult();
                Assertions.assertNotNull(partial);
                Assertions.assertEquals(status, partial.getRemoteExecutionDetails().getHttpStatus());
                Assertions.assertEquals(status == 200 ? Outcome.PROTOCOL_FAILURE : Outcome.HTTP_FAILURE,
                    partial.getRemoteExecutionDetails().getOutcome());
                Assertions.assertEquals(ExitCodeSource.UNAVAILABLE, partial.getRemoteExecutionDetails().getExitCodeSource());
                Assertions.assertNull(partial.getRemoteRawHttpBody());
                Assertions.assertFalse(failure.toString().contains(canary));
            }
        }
    }

    @Test
    void httpFailureRetainsItsStatusWithoutCreatingAProcessExit() throws IOException {
        String body = "<html>fixture-proxy-error</html>";
        try (JsonCollectStub stub = JsonCollectStub.start(body, 503)) {
            OpenCliProperties props = baseProps(stub.port());
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                OpenCliExecutableFailureException failure = Assertions.assertThrows(
                    OpenCliExecutableFailureException.class, () -> client.collect(request));
                OpenCliResult partial = failure.getPartialResult();
                Assertions.assertNotNull(partial);
                Assertions.assertNull(partial.getRemoteRawHttpBody());
                Assertions.assertNull(partial.getExitCode());
                OpenCliRemoteExecutionDetails details = partial.getRemoteExecutionDetails();
                Assertions.assertNotNull(details);
                Assertions.assertEquals(503, details.getHttpStatus());
                Assertions.assertNull(details.getLegacySuccess());
                Assertions.assertEquals(Outcome.HTTP_FAILURE, details.getOutcome());
                Assertions.assertEquals(ExitCodeSource.UNAVAILABLE, details.getExitCodeSource());
                Assertions.assertFalse(details.getObservedRemoteExitCode().isPresent());

                props.setRemoteCaptureRawHttpResponse(true);
                OpenCliExecutableFailureException withCapture = Assertions.assertThrows(
                    OpenCliExecutableFailureException.class, () -> client.collect(request));
                Assertions.assertEquals(body, withCapture.getPartialResult().getRemoteRawHttpBody());
                Assertions.assertFalse(withCapture.getMessage().contains("fixture-proxy-error"));
            }
        }
    }

    @Test
    void authenticationHeaderIsSentOnlyOverVerifiedTls(@TempDir Path temporary) throws Exception {
        String credential = "sdk-credential-" + UUID.randomUUID();
        String header = "X-OpenCLI-Test-Key";
        AtomicReference<String> receivedCredential = new AtomicReference<>();
        AtomicReference<String> receivedUri = new AtomicReference<>();
        OpenCliTlsFixture tls = OpenCliTlsFixture.create(temporary);
        SSLContext previous = SSLContext.getDefault();
        try (JsonCollectStub stub = JsonCollectStub.start(tls.getServer(), "/agent/collect", exchange -> {
            receivedCredential.set(exchange.getRequestHeaders().getFirst(header));
            receivedUri.set(exchange.getRequestURI().toASCIIString());
            // The synthetic fixture supports only this explicit header.
            boolean accepted = credential.equals(receivedCredential.get());
            byte[] response = (accepted ? "{\"success\":true}" : "credential required " + receivedCredential.get())
                .getBytes(StandardCharsets.UTF_8);
            if (!accepted) {
                exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=fixture");
            }
            exchange.sendResponseHeaders(accepted ? 200 : 401, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        })) {
            OpenCliProperties props = baseProps(stub.port());
            props.setRemoteAgentBaseUrl("https://127.0.0.1:" + stub.port() + "/agent///");
            props.setRemoteAuthentication(new OpenCliRemoteAuthentication(header, credential));
            Assertions.assertFalse(props.toString().contains(credential));
            Assertions.assertNull(props.copyForLocalCliExecution().getRemoteAuthentication());
            OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
            try (OpenCliRemoteAgentHttpClient untrusted = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliExecutableFailureException failure = Assertions.assertThrows(
                    OpenCliExecutableFailureException.class, () -> untrusted.collect(request));
                Assertions.assertFalse(failure.toString().contains(credential));
                Assertions.assertEquals(0, stub.requests());
            }

            SSLContext.setDefault(tls.getTrustedClient());
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                Assertions.assertTrue(client.collect(request).isSuccess());
                Assertions.assertEquals(credential, receivedCredential.get());
                Assertions.assertEquals("/agent/collect", receivedUri.get());
                Assertions.assertFalse(stub.requestBody.contains(credential));
                props.setRemoteAuthentication(new OpenCliRemoteAuthentication(header, "rejected-" + credential));
                ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
                PrintStream originalError = System.err;
                try (PrintStream capture = new PrintStream(diagnostics, true, StandardCharsets.UTF_8.name())) {
                    System.setErr(capture);
                    OpenCliExecutableFailureException denied = Assertions.assertThrows(
                        OpenCliExecutableFailureException.class, () -> client.collect(request));
                    StringWriter stack = new StringWriter();
                    denied.printStackTrace(new PrintWriter(stack));
                    Assertions.assertFalse(stack.toString().contains(credential));
                    Assertions.assertNull(denied.getPartialResult().getRemoteRawHttpBody());
                } finally {
                    System.setErr(originalError);
                }
                String log = diagnostics.toString(StandardCharsets.UTF_8.name());
                Assertions.assertTrue(log.contains("status=401"));
                Assertions.assertFalse(log.contains(credential));
                Assertions.assertEquals(2, stub.requests());

                props.setRemoteAuthentication(null);
                OpenCliExecutableFailureException failure = Assertions.assertThrows(
                    OpenCliExecutableFailureException.class, () -> client.collect(request));
                Assertions.assertEquals(401, failure.getPartialResult().getRemoteExecutionDetails().getHttpStatus());
                Assertions.assertNull(receivedCredential.get());
                Assertions.assertEquals(3, stub.requests());
            }
        } finally {
            SSLContext.setDefault(previous);
        }
    }

    @Test
    void rejectsConfiguredAuthenticationOnPlainHttpBeforeDispatch() throws IOException {
        String credential = "private-token";
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true}")) {
            OpenCliProperties props = baseProps(stub.port());
            props.setRemoteAuthentication(new OpenCliRemoteAuthentication("Authorization", "Bearer " + credential));
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                OpenCliExecutableFailureException failure = Assertions.assertThrows(
                    OpenCliExecutableFailureException.class, () -> client.collect(request));
                Assertions.assertFalse(failure.toString().contains(credential));
                Assertions.assertEquals(0, stub.requests());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"user-info", "query", "fragment"})
    void rejectsSecretsInAnAgentBaseUrlBeforeDispatch(String input) throws IOException {
        String credential = "url-secret-" + UUID.randomUUID();
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true}")) {
            OpenCliProperties props = baseProps(stub.port());
            String authority = "127.0.0.1:" + stub.port();
            if ("user-info".equals(input)) {
                props.setRemoteAgentBaseUrl("http://operator:" + credential + "@" + authority);
            } else {
                props.setRemoteAgentBaseUrl("http://" + authority + "/collect"
                    + ("query".equals(input) ? "?key=" : "#") + credential);
            }
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                OpenCliExecutableFailureException failure = Assertions.assertThrows(
                    OpenCliExecutableFailureException.class, () -> client.collect(request));
                Assertions.assertFalse(failure.toString().contains(credential));
                Assertions.assertNull(failure.getCause());
                Assertions.assertEquals(0, stub.requests());
            }
        }
    }

    @Test
    void rejectsMalformedAuthenticationHeaderWithoutRevealingItsValue() {
        String secret = "fixture-injection-canary";
        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
            () -> new OpenCliRemoteAuthentication("Authorization", "Bearer " + secret + "\r\nX-Injected: yes"));
        Assertions.assertFalse(failure.toString().contains(secret));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> new OpenCliRemoteAuthentication("Host", secret));
    }

    @Test
    void nonSuccessHttpResponseDoesNotExposeBodyCanaryInException() throws IOException {
        String canary = "body-secret-" + UUID.randomUUID();
        try (JsonCollectStub stub = JsonCollectStub.start(canary, 503)) {
            OpenCliProperties props = baseProps(stub.port());
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
            String logText = diagnostics.toString(StandardCharsets.UTF_8.name());
            Assertions.assertTrue(logText.contains("Agent HTTP non-2xx"));
            Matcher correlation = Pattern.compile("correlationId=([0-9a-f-]{36})")
                .matcher(ex.getMessage());
            Assertions.assertTrue(correlation.find());
            Assertions.assertDoesNotThrow(() -> UUID.fromString(correlation.group(1)));
            Assertions.assertTrue(logText.contains(correlation.group(0)));
            Assertions.assertFalse(logText.contains(canary));
            for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
                Assertions.assertFalse(cause.toString().contains(canary));
            }
        }
    }

    @Test
    void requestSerializationFailureIsPrivateAndNeverDispatched() throws IOException {
        String canary = "serialization-private-" + UUID.randomUUID();
        try (JsonCollectStub stub = JsonCollectStub.start("{\"success\":true}");
             OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(stub.port()))) {
            OpenCliCollectRequest request = new OpenCliCollectRequest() {
                @Override
                public Map<String, Object> getArgs() {
                    throw new IllegalStateException(canary);
                }
            };
            ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
            PrintStream previous = System.err;
            OpenCliExecutableFailureException failure;
            try (PrintStream capture = new PrintStream(diagnostics, true, StandardCharsets.UTF_8.name())) {
                System.setErr(capture);
                try {
                    failure = Assertions.assertThrows(OpenCliExecutableFailureException.class, () -> client.collect(request));
                    failure.printStackTrace(capture);
                } finally {
                    System.setErr(previous);
                }
            }
            String output = diagnostics.toString(StandardCharsets.UTF_8.name());
            Assertions.assertTrue(failure.getMessage().contains("Failed to serialize collect request"));
            Assertions.assertTrue(output.contains("Agent request serialization failed correlationId="));
            Assertions.assertFalse(output.contains(canary));
            Assertions.assertEquals(0, stub.requests());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void responseCloseFailureProtectsProviderDetailsAndPrimaryOutcome(boolean validEnvelope) throws Exception {
        String canary = "response-close-private-" + UUID.randomUUID();
        String body = validEnvelope ? "{\"success\":true}" : "null";
        CloseFailureResponse response = new CloseFailureResponse(body, canary);
        try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(1))) {
            setTransport(client, response, null);
            OpenCliExecutableFailureException failure = Assertions.assertThrows(
                OpenCliExecutableFailureException.class, () -> client.collect(new OpenCliCollectRequest()));
            OpenCliRemoteExecutionDetails details = failure.getPartialResult().getRemoteExecutionDetails();
            Assertions.assertEquals(200, details.getHttpStatus());
            Assertions.assertEquals(validEnvelope ? Outcome.TRANSPORT_FAILURE : Outcome.PROTOCOL_FAILURE, details.getOutcome());
            Assertions.assertEquals(1, response.closeCalls);
            StringWriter stack = new StringWriter();
            failure.printStackTrace(new PrintWriter(stack));
            Assertions.assertFalse(stack.toString().contains(canary));
        }
    }

    @Test
    void responseItemsMappingFailureKeepsStatusAndProtectsDiagnostic() throws Exception {
        String body = "{\"success\":true,\"items\":[[\"mapping-private-canary\"]]}";
        try (JsonCollectStub stub = JsonCollectStub.start(body)) {
            OpenCliProperties properties = baseProps(stub.port());
            properties.setRemoteCaptureRawHttpResponse(true);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(properties)) {
                JsonFactory factory = JsonFactory.builder()
                    .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(1).build()).build();
                Field writer = OpenCliRemoteAgentHttpClient.class.getDeclaredField("responseItemsWriter");
                writer.setAccessible(true);
                writer.set(client, JsonMapper.builder(factory).build().writerWithDefaultPrettyPrinter());

                OpenCliRemoteProtocolException failure = Assertions.assertThrows(
                    OpenCliRemoteProtocolException.class,
                    () -> client.collect(new OpenCliCollectRequest()));
                OpenCliResult partial = failure.getPartialResult();
                Assertions.assertEquals(200, partial.getRemoteExecutionDetails().getHttpStatus());
                Assertions.assertEquals(Outcome.PROTOCOL_FAILURE, partial.getRemoteExecutionDetails().getOutcome());
                Assertions.assertNull(partial.getExitCode());
                Assertions.assertEquals(body, partial.getRemoteRawHttpBody());
                StringWriter stack = new StringWriter();
                failure.printStackTrace(new PrintWriter(stack));
                Assertions.assertFalse(stack.toString().contains("mapping-private-canary"));
                Assertions.assertEquals(1, stub.requests());
            }
        }
    }

    @Test
    void transportCloseFailureDoesNotExposeProviderDetails() throws Exception {
        String canary = "transport-close-private-" + UUID.randomUUID();
        OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(1));
        setTransport(client, null, new IllegalStateException(canary));
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        PrintStream previous = System.err;
        try (PrintStream capture = new PrintStream(diagnostics, true, StandardCharsets.UTF_8.name())) {
            System.setErr(capture);
            try {
                Assertions.assertDoesNotThrow(client::close);
            } finally {
                System.setErr(previous);
            }
        }
        String output = diagnostics.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(output.contains("Agent HTTP transport close failed"));
        Assertions.assertFalse(output.contains(canary));
    }

    // Replace the native provider to inject close faults that a loopback server cannot reliably schedule.
    private static void setTransport(OpenCliRemoteAgentHttpClient client, CloseableHttpResponse response,
        RuntimeException shutdownFailure) throws ReflectiveOperationException {
        Field transport = OpenCliRemoteAgentHttpClient.class.getDeclaredField("transport");
        transport.setAccessible(true);
        transport.set(client, new CloseableHttpClient() {
            @Override
            protected CloseableHttpResponse doExecute(HttpHost host, HttpRequest request, HttpContext context) {
                return response;
            }

            @Override
            public HttpParams getParams() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ClientConnectionManager getConnectionManager() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {
                if (Objects.nonNull(shutdownFailure)) {
                    throw shutdownFailure;
                }
            }
        });
    }

    private static final class CloseFailureResponse extends BasicHttpResponse implements CloseableHttpResponse {
        private final String canary;
        private int closeCalls;

        private CloseFailureResponse(String body, String canary) {
            super(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
            this.canary = canary;
            setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            closeCalls++;
            throw new IOException(canary);
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
    void disconnectAfterAcceptingCollectDoesNotReplayOrInventAnExit() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start(HttpExchange::close);
             OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(baseProps(stub.port()))) {
            OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
            OpenCliExecutableFailureException failure = Assertions.assertThrows(
                OpenCliExecutableFailureException.class, () -> client.collect(request));
            Assertions.assertEquals(1, stub.requests());
            OpenCliResult partial = failure.getPartialResult();
            Assertions.assertNotNull(partial);
            Assertions.assertFalse(partial.isSuccess());
            Assertions.assertNull(partial.getExitCode());
            Assertions.assertNull(partial.getRemoteRawHttpBody());
            OpenCliRemoteExecutionDetails details = partial.getRemoteExecutionDetails();
            Assertions.assertEquals(Outcome.TRANSPORT_FAILURE, details.getOutcome());
            Assertions.assertTrue(details.isRemoteOutcomeUnknown());
            Assertions.assertFalse(details.getObservedHttpStatus().isPresent());
            Assertions.assertFalse(details.getObservedRemoteExitCode().isPresent());
            Assertions.assertFalse(OpenCliDiagnosticExport.export(partial, 512).contains("httpStatus="));
            Assertions.assertNull(failure.getCause());
        }
    }

    @Test
    void truncatedResponseBodyRetainsOnlyObservedHttpStatus() throws IOException {
        try (JsonCollectStub stub = JsonCollectStub.start(exchange -> {
            exchange.sendResponseHeaders(200, 64);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write('{');
            } finally {
                exchange.close();
            }
        })) {
            OpenCliProperties props = baseProps(stub.port());
            props.setRemoteCaptureRawHttpResponse(true);
            try (OpenCliRemoteAgentHttpClient client = new OpenCliRemoteAgentHttpClient(props)) {
                OpenCliCollectRequest request = OpenCliCollectRequest.builder().site("npm").command("search").build();
                OpenCliExecutableFailureException failure = Assertions.assertThrows(
                    OpenCliExecutableFailureException.class, () -> client.collect(request));
                Assertions.assertEquals(1, stub.requests());
                OpenCliResult partial = failure.getPartialResult();
                Assertions.assertNotNull(partial);
                OpenCliRemoteExecutionDetails details = partial.getRemoteExecutionDetails();
                Assertions.assertEquals(Outcome.TRANSPORT_FAILURE, details.getOutcome());
                Assertions.assertTrue(details.isRemoteOutcomeUnknown());
                Assertions.assertEquals(200, details.getObservedHttpStatus().getAsInt());
                Assertions.assertFalse(details.getObservedRemoteExitCode().isPresent());
                Assertions.assertNull(partial.getExitCode());
                Assertions.assertNull(partial.getRemoteRawHttpBody());
            }
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
        "npm|search|react|--limit", "npm|search|react|--limit|--bogus",
        "npm|search|react|--verbose",
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
            OpenCliRemoteExecutionDetails details = result.getRemoteExecutionDetails();
            Assertions.assertNotNull(details);
            Assertions.assertEquals(Outcome.SUCCESS, details.getOutcome());
            Assertions.assertEquals(Boolean.TRUE, details.getLegacySuccess());
            Assertions.assertEquals(ItemsSource.ABSENT_DEFAULT, details.getItemsSource());
            Assertions.assertEquals(Integer.valueOf(0), result.getExitCode());
            Assertions.assertFalse(details.getObservedRemoteExitCode().isPresent());
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
            OpenCliResult partial = failure.getPartialResult();
            Assertions.assertNotNull(partial);
            Assertions.assertNull(partial.getExitCode());
            Assertions.assertEquals(200, partial.getRemoteExecutionDetails().getHttpStatus());
            Assertions.assertEquals(Outcome.PROTOCOL_FAILURE, partial.getRemoteExecutionDetails().getOutcome());
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
            return start(exchange -> {
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
        }

        static JsonCollectStub start(HttpHandler response) throws IOException {
            return start(null, response);
        }

        static JsonCollectStub start(SSLContext tls, HttpHandler response) throws IOException {
            return start(tls, "/collect", response);
        }

        static JsonCollectStub start(SSLContext tls, String path, HttpHandler response) throws IOException {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
            HttpServer server;
            if (Objects.isNull(tls)) {
                server = HttpServer.create(address, 0);
            } else {
                HttpsServer https = HttpsServer.create(address, 0);
                https.setHttpsConfigurator(new HttpsConfigurator(tls));
                server = https;
            }
            JsonCollectStub stub = new JsonCollectStub(server, server.getAddress().getPort());
            server.createContext(
                path,
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
                        response.handle(exchange);
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
