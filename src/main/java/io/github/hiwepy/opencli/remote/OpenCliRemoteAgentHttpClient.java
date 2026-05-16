package io.github.hiwepy.opencli.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencli.OpenCliProperties;
import io.github.hiwepy.opencli.core.OpenCliOutputParser;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.exception.OpenCliExecutableFailureException;
import io.github.hiwepy.opencli.exception.OpenCliNonZeroExitException;
import io.github.hiwepy.opencli.parser.OpenCliParsedFields;
import io.github.hiwepy.opencli.util.OpenCliStrings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * 调用 opencli-admin 边缘 Agent 的 HTTP {@code POST /collect}。
 * <p>
 * 与 {@code backend/agent_server.py} 对齐；响应中的 {@code items} 已由 Agent 解析为结构化行，
 * 本客户端将其序列化为 JSON 写入 {@link OpenCliResult#getStdout()}，便于与本地 {@code -f json}
 * 输出链路复用同一解析代码。
 * </p>
 */
@Slf4j
public final class OpenCliRemoteAgentHttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OpenCliProperties properties;

    private final HttpClient httpClient;

    /**
     * @param properties 含 {@link OpenCliProperties#getRemoteAgentBaseUrl()} 等
     */
    public OpenCliRemoteAgentHttpClient(OpenCliProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.httpClient = HttpClient.newBuilder().build();
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
        long timeoutMs = properties.getCommandTimeoutMillis();
        if (timeoutMs <= 0) {
            timeoutMs = 300_000L;
        }
        byte[] body;
        try {
            body = MAPPER.writeValueAsBytes(request);
        } catch (IOException e) {
            throw new OpenCliExecutableFailureException("Failed to serialize collect request: " + e.getMessage(), e);
        }
        HttpRequest httpRequest =
            HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = resp.statusCode();
            String respBody = resp.body() == null ? "" : resp.body();
            if (status < 200 || status >= 300) {
                throw new OpenCliExecutableFailureException(
                    "Agent HTTP " + status + " from " + url + ": " + respBody.substring(0, Math.min(500, respBody.length())), null);
            }
            return mapResponse(respBody);
        } catch (OpenCliNonZeroExitException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenCliExecutableFailureException("Interrupted during agent HTTP call: " + url, e);
        } catch (IOException e) {
            log.warn("Agent HTTP failed url={} message={}", url, e.getMessage());
            throw new OpenCliExecutableFailureException("Agent HTTP I/O error: " + url + " — " + e.getMessage(), e);
        }
    }

    private OpenCliResult mapResponse(String respBody) throws IOException {
        String rawCapture = captureRawIfEnabled(respBody);
        AgentCollectEnvelope env = MAPPER.readValue(respBody, AgentCollectEnvelope.class);
        boolean success = env.success != null && env.success;
        String err = env.error == null ? "" : env.error;
        String stdout;
        if (env.items == null || env.items.isNull()) {
            stdout = "[]";
        } else if (env.items.isArray()) {
            stdout = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(env.items);
        } else {
            stdout = env.items.toPrettyString();
        }
        OpenCliParsedFields parsed = OpenCliOutputParser.parseBestEffort(stdout, err);
        if (!success) {
            OpenCliResult failed =
                OpenCliResult.builder()
                    .stdout(stdout)
                    .stderr(err)
                    .exitCode(1)
                    .success(false)
                    .parsed(parsed)
                    .remoteRawHttpBody(rawCapture)
                    .build();
            throw new OpenCliNonZeroExitException("OpenCLI agent reported failure: " + err, failed);
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

    /**
     * 按配置决定是否保留 Agent 响应原文（用于 {@link OpenCliResult#getRemoteRawHttpBody()}）。
     *
     * @param respBody HTTP 响应体字符串
     * @return 需要保留时返回原文，否则 null
     */
    private String captureRawIfEnabled(String respBody) {
        if (!properties.isRemoteCaptureRawHttpResponse()) {
            return null;
        }
        return respBody;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class AgentCollectEnvelope {

        public Boolean success;

        public JsonNode items;

        public String error;
    }
}
