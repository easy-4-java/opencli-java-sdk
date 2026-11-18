package io.github.easy4j.opencli.remote;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliOutputParser;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliRemoteProtocolException;
import io.github.easy4j.opencli.exception.OpenCliRemoteResponseLimitException;
import io.github.easy4j.opencli.exception.OpenCliRemoteTimeoutException;
import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/** HTTP client for the opencli-admin legacy {@code POST /collect} contract. */
@Slf4j
public final class OpenCliRemoteAgentHttpClient implements AutoCloseable {

    private static final JsonMapper MAPPER = new JsonMapper();
    private static final ObjectReader ENVELOPE_READER = MAPPER.reader().with(
        DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
        DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

    private static final ScheduledThreadPoolExecutor DEADLINES = new ScheduledThreadPoolExecutor(1, task -> {
        Thread thread = new Thread(task, "opencli-http-deadline");
        thread.setDaemon(true);
        return thread;
    });

    static {
        DEADLINES.setRemoveOnCancelPolicy(true);
    }

    private final OpenCliProperties properties;

    private final Object transportLock = new Object();
    private CloseableHttpClient transport;
    private boolean closed;

    /**
     * @param properties 含 {@code remoteAgentBaseUrl} 等配置
     */
    public OpenCliRemoteAgentHttpClient(OpenCliProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * 执行一次远程采集。
     *
     * @param request collect 请求体，不得为 null
     * @return 与本地执行语义尽量一致的 {@link OpenCliResult}
     */
    public OpenCliResult collect(OpenCliCollectRequest request) {
        return collect(request, System.nanoTime());
    }

    /**
     * Collects within the budget of an invocation that has already performed local preparation.
     *
     * @param request legacy collect request
     * @param submittedAtNanos invocation start sampled from this JVM's {@link System#nanoTime()}
     * @return the legacy collect result
     */
    public OpenCliResult collect(OpenCliCollectRequest request, long submittedAtNanos) {
        Objects.requireNonNull(request, "request");
        String base = properties.getRemoteAgentBaseUrl();
        if (OpenCliStrings.isBlank(base)) {
            throw new IllegalStateException("opencli.remote-agent-base-url must be set for REMOTE_AGENT_HTTP");
        }
        int timeout = resolveTimeoutMillis();
        long deadline = submittedAtNanos + TimeUnit.MILLISECONDS.toNanos(timeout);
        int maxResponseBytes = properties.getMaxRemoteResponseBytes();
        if (maxResponseBytes <= 0) {
            throw new IllegalArgumentException("maxRemoteResponseBytes must be positive");
        }
        String correlationId = UUID.randomUUID().toString();
        String bodyJson;
        try {
            bodyJson = MAPPER.writeValueAsString(request);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Agent request serialization failed correlationId={}", correlationId);
            throw failure("Failed to serialize collect request", correlationId);
        }

        HttpPost post;
        try {
            post = new HttpPost(base.trim().replaceAll("/+$", "") + "/collect");
        } catch (IllegalArgumentException exception) {
            throw failure("Invalid agent HTTP endpoint", correlationId);
        }
        post.setEntity(new StringEntity(bodyJson, ContentType.APPLICATION_JSON));
        CloseableHttpClient client = transport();
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) {
            throw new OpenCliRemoteTimeoutException(timeout, false, correlationId);
        }
        int remainingMillis = (int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
        post.setConfig(RequestConfig.custom()
            .setConnectionRequestTimeout(remainingMillis)
            .setConnectTimeout(remainingMillis)
            .setSocketTimeout(remainingMillis)
            .build());

        ScheduledFuture<?> abort = DEADLINES.schedule(post::abort, deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
        try (CloseableHttpResponse response = client.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            Optional<String> body = readBoundedBody(response.getEntity(), maxResponseBytes);
            if (post.isAborted() || deadline - System.nanoTime() <= 0) {
                throw new OpenCliRemoteTimeoutException(timeout, true, correlationId);
            }
            String respBody = body.orElseThrow(
                () -> new OpenCliRemoteResponseLimitException(status, maxResponseBytes, correlationId));
            if (status < 200 || status >= 300) {
                log.warn("Agent HTTP non-2xx correlationId={} status={} responseChars={}",
                    correlationId, status, respBody.length());
                throw failure("Agent HTTP " + status, correlationId);
            }
            return mapResponse(respBody, correlationId);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Agent response parse failed correlationId={}", correlationId);
            throw failure("Failed to parse agent response", correlationId);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            throw new OpenCliRemoteTimeoutException(timeout, true, correlationId);
        } catch (IOException | IllegalArgumentException e) {
            if (post.isAborted() || deadline - System.nanoTime() <= 0) {
                throw new OpenCliRemoteTimeoutException(timeout, true, correlationId);
            }
            log.warn("Agent HTTP I/O failed correlationId={}", correlationId);
            throw failure("Agent HTTP I/O error", correlationId);
        } finally {
            abort.cancel(false);
        }
    }

    private CloseableHttpClient transport() {
        synchronized (transportLock) {
            if (closed) {
                throw new IllegalStateException("OpenCLI remote agent HTTP transport is closed");
            }
            if (Objects.isNull(transport)) {
                transport = HttpClients.custom()
                    .useSystemProperties()
                    .disableAutomaticRetries()
                    .disableRedirectHandling()
                    .setMaxConnTotal(200)
                    .setMaxConnPerRoute(20)
                    .evictIdleConnections(30, TimeUnit.SECONDS)
                    .build();
            }
            return transport;
        }
    }

    /** Closes this client's HTTP pool and aborts its outstanding requests. */
    @Override
    public void close() {
        CloseableHttpClient client;
        synchronized (transportLock) {
            closed = true;
            client = transport;
            transport = null;
        }
        if (Objects.nonNull(client)) {
            try {
                client.close();
            } catch (IOException e) {
                log.warn("Agent HTTP transport close failed");
            }
        }
    }

    private static Optional<String> readBoundedBody(HttpEntity entity, int limit) throws IOException {
        if (Objects.isNull(entity)) {
            return Optional.of("");
        }
        // The response owns the stream. Closing the stream itself can drain an oversized entity.
        InputStream input = entity.getContent();
        byte[] buffer = new byte[Math.min(8192, limit)];
        ByteArrayOutputStream body = new ByteArrayOutputStream(buffer.length);
        while (true) {
            int remaining = limit - body.size();
            int length = (int) Math.min(buffer.length, (long) remaining + 1);
            int read = input.read(buffer, 0, length);
            if (read < 0) {
                break;
            }
            if (read > remaining) {
                return Optional.empty();
            }
            body.write(buffer, 0, read);
        }
        ContentType type = ContentType.get(entity);
        Charset charset = Objects.nonNull(type) ? type.getCharset() : null;
        return Optional.of(body.toString(Objects.nonNull(charset) ? charset.name() : StandardCharsets.UTF_8.name()));
    }

    private int resolveTimeoutMillis() {
        long timeoutMs = properties.getCommandTimeoutMillis();
        if (timeoutMs <= 0) {
            timeoutMs = 300_000L;
        }
        return (int) Math.min(timeoutMs, Integer.MAX_VALUE);
    }

    private static OpenCliExecutableFailureException failure(String message, String correlationId) {
        return new OpenCliExecutableFailureException(message + " correlationId=" + correlationId, null);
    }

    private OpenCliResult mapResponse(String respBody, String correlationId)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        String rawCapture = captureRawIfEnabled(respBody);
        JsonNode envelope;
        try {
            envelope = ENVELOPE_READER.readTree(respBody);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Agent response envelope parse failed correlationId={} responseChars={}",
                correlationId, respBody.length());
            throw new OpenCliRemoteProtocolException("Failed to parse agent response envelope", correlationId);
        }
        if (Objects.isNull(envelope) || !envelope.isObject() || !envelope.path("success").isBoolean()) {
            log.warn("Agent response envelope status invalid correlationId={} responseChars={}",
                correlationId, respBody.length());
            throw new OpenCliRemoteProtocolException("Invalid agent response envelope status", correlationId);
        }
        JsonNode error = envelope.path("error");
        if (!error.isMissingNode() && !error.isNull() && !error.isTextual()) {
            log.warn("Agent response envelope error field invalid correlationId={} responseChars={}",
                correlationId, respBody.length());
            throw new OpenCliRemoteProtocolException("Invalid agent response envelope error field", correlationId);
        }
        boolean success = envelope.path("success").booleanValue();
        String err = error.isTextual() ? error.asText() : "";
        JsonNode items = envelope.path("items");
        String stdout;
        if (items.isMissingNode() || items.isNull()) {
            stdout = "[]";
        } else if (items.isArray()) {
            stdout = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(items);
        } else {
            stdout = items.toPrettyString();
        }
        OpenCliParsedFields parsed = OpenCliOutputParser.parseBestEffort(stdout, err);
        if (!success) {
            log.warn(
                "Agent collect reported failure correlationId={} errorChars={} stdoutLength={}",
                correlationId,
                err.length(),
                stdout.length());
            OpenCliResult failed =
                OpenCliResult.builder()
                    .stdout(stdout)
                    .stderr(err)
                    .exitCode(1)
                    .success(false)
                    .parsed(parsed)
                    .remoteRawHttpBody(rawCapture)
                    .build();
            throw new OpenCliNonZeroExitException("OpenCLI agent reported failure correlationId=" + correlationId, failed);
        }
        return OpenCliResult.builder()
            .stdout(stdout)
            .stderr(err.isEmpty() ? "" : err)
            .exitCode(0)
            .success(true)
            .parsed(parsed)
            .remoteRawHttpBody(rawCapture)
            .build();
    }

    private String captureRawIfEnabled(String respBody) {
        if (!properties.isRemoteCaptureRawHttpResponse()) {
            return null;
        }
        return respBody;
    }

}
