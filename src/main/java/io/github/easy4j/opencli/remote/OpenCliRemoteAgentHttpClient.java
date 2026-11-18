package io.github.easy4j.opencli.remote;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliOutputParser;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliRemoteProtocolException;
import io.github.easy4j.opencli.exception.OpenCliRemoteResponseLimitException;
import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import kong.unirest.HttpResponse;
import kong.unirest.RawResponse;
import kong.unirest.UnrecoverableException;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;

/**
 * 调用 opencli-admin 边缘 Agent 的 HTTP {@code POST /collect}。
 * <p>
 * 使用 {@code com.konghq:unirest-java} 3.x（与 openclaw-java-sdk 同系列，且兼容 JDK 8 的 2.3.x 线）。
 * </p>
 */
@Slf4j/**

 * HTTP client that calls the opencli-admin edge Agent's {@code POST /collect} endpoint.
 *
 * <p>Uses {@code com.konghq:unirest-java} 3.x (same series as openclaw-java-sdk,
 * compatible with JDK 8 via the 2.3.x line).</p>

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class OpenCliRemoteAgentHttpClient implements AutoCloseable {

    private static final JsonMapper MAPPER = new JsonMapper();
    private static final ObjectReader ENVELOPE_READER = MAPPER.reader().with(
        DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
        DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

    private final OpenCliProperties properties;

    private final Object transportLock = new Object();
    private volatile kong.unirest.UnirestInstance transport;
    private volatile boolean closed;

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
        Objects.requireNonNull(request, "request");
        String base = properties.getRemoteAgentBaseUrl();
        if (OpenCliStrings.isBlank(base)) {
            throw new IllegalStateException("opencli.remote-agent-base-url must be set for REMOTE_AGENT_HTTP");
        }
        String url = base.trim().replaceAll("/+$", "") + "/collect";
        int timeout = resolveTimeoutMillis();
        int maxResponseBytes = properties.getMaxRemoteResponseBytes();
        if (maxResponseBytes <= 0) {
            throw new IllegalArgumentException("maxRemoteResponseBytes must be positive");
        }
        String correlationId = UUID.randomUUID().toString();
        String bodyJson;
        try {
            bodyJson = MAPPER.writeValueAsString(request);
        } catch (JacksonException e) {
            log.warn("Agent request serialization failed correlationId={}", correlationId);
            throw failure("Failed to serialize collect request", correlationId);
        }
        try {
            HttpResponse<Optional<String>> response =
                transport()
                    .post(url)
                    .connectTimeout(timeout)
                    .socketTimeout(timeout)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(bodyJson)
                    .asObject(raw -> readBoundedBody(raw, maxResponseBytes));
            int status = response.getStatus();
            if (response.getParsingError().isPresent()) {
                log.warn("Agent HTTP body read failed correlationId={} status={}", correlationId, status);
                throw failure("Agent HTTP I/O error", correlationId);
            }
            String respBody = response.getBody().orElseThrow(
                () -> new OpenCliRemoteResponseLimitException(status, maxResponseBytes, correlationId));
            if (status < 200 || status >= 300) {
                log.warn("Agent HTTP non-2xx correlationId={} status={} responseChars={}",
                    correlationId, status, respBody.length());
                throw failure("Agent HTTP " + status, correlationId);
            }
            return mapResponse(respBody, correlationId);
        } catch (OpenCliNonZeroExitException e) {
            throw e;
        } catch (JacksonException e) {
            log.warn("Agent response parse failed correlationId={}", correlationId);
            throw failure("Failed to parse agent response", correlationId);
        } catch (UnirestException e) {
            log.warn("Agent HTTP I/O failed correlationId={}", correlationId);
            throw failure("Agent HTTP I/O error", correlationId);
        }
    }

    /**
     * 懒加载本客户端私有的 Unirest 实例（{@code Unirest.spawn}）——
     * shutdown 只影响自身，不触碰 JVM 全局主实例。
     */
    private kong.unirest.UnirestInstance transport() {
        if (closed) {
            throw new IllegalStateException("OpenCLI remote agent HTTP transport is closed");
        }
        kong.unirest.UnirestInstance instance = transport;
        if (Objects.isNull(instance)) {
            synchronized (transportLock) {
                if (Objects.isNull(transport)) {
                    transport = kong.unirest.Unirest.spawnInstance();
                }
                instance = transport;
            }
        }
        return instance;
    }

    /**
     * 关闭本客户端的 Unirest 实例（幂等）。close 之后远程调用抛
     * {@link IllegalStateException}；JVM 全局主实例不受影响。
     */
    @Override
    public void close() {
        closed = true;
        kong.unirest.UnirestInstance instance = transport;
        if (Objects.nonNull(instance)) {
            synchronized (transportLock) {
                instance = transport;
            }
        }
        if (Objects.nonNull(instance)) {
            instance.shutDown();
        }
    }

    private static Optional<String> readBoundedBody(RawResponse response, int limit) {
        try {
            // Unirest owns the response stream. Closing it here can drain an oversized HTTP entity.
            InputStream input = response.getContent();
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
            String contentType = response.getContentType();
            Charset charset = OpenCliStrings.isBlank(contentType) ? null : ContentType.parse(contentType).getCharset();
            String encoding = Objects.nonNull(charset) ? charset.name() : response.getConfig().getDefaultResponseEncoding();
            return Optional.of(body.toString(encoding));
        } catch (IOException | RuntimeException exception) {
            // Avoid Unirest's fallback, which otherwise retries by reading the full body for diagnostics.
            throw new UnrecoverableException(exception);
        }
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

    private OpenCliResult mapResponse(String respBody, String correlationId) {
        String rawCapture = captureRawIfEnabled(respBody);
        JsonNode envelope;
        try {
            envelope = ENVELOPE_READER.readTree(respBody);
        } catch (JacksonException e) {
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
