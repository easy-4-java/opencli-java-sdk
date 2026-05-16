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
import java.util.Objects;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import lombok.extern.slf4j.Slf4j;

/**
 * 调用 opencli-admin 边缘 Agent 的 HTTP {@code POST /collect}。
 * <p>
 * 使用 Unirest（与 {@code openclaw-java-sdk} 依赖一致），JDK 8+ 可用，不依赖 {@code java.net.http}。
 * </p>
 */
@Slf4j
public final class OpenCliRemoteAgentHttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OpenCliProperties properties;

    /**
     * @param properties 含 {@link OpenCliProperties#getRemoteAgentBaseUrl()} 等
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
        String bodyJson;
        try {
            bodyJson = MAPPER.writeValueAsString(request);
        } catch (IOException e) {
            throw new OpenCliExecutableFailureException("Failed to serialize collect request: " + e.getMessage(), e);
        }
        try {
            HttpResponse<String> response =
                Unirest.post(url)
                    .requestTimeout(timeout)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(bodyJson)
                    .asString();
            int status = response.getStatus();
            String respBody = response.getBody() == null ? "" : response.getBody();
            if (status < 200 || status >= 300) {
                throw new OpenCliExecutableFailureException(
                    "Agent HTTP "
                        + status
                        + " from "
                        + url
                        + ": "
                        + respBody.substring(0, Math.min(500, respBody.length())),
                    null);
            }
            return mapResponse(respBody);
        } catch (OpenCliNonZeroExitException e) {
            throw e;
        } catch (IOException e) {
            throw new OpenCliExecutableFailureException("Failed to parse agent response: " + e.getMessage(), e);
        } catch (UnirestException e) {
            log.warn("Agent HTTP failed url={} message={}", url, e.getMessage());
            throw new OpenCliExecutableFailureException("Agent HTTP I/O error: " + url + " — " + e.getMessage(), e);
        }
    }

    private int resolveTimeoutMillis() {
        long timeoutMs = properties.getCommandTimeoutMillis();
        if (timeoutMs <= 0) {
            timeoutMs = 300_000L;
        }
        return (int) Math.min(timeoutMs, Integer.MAX_VALUE);
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
