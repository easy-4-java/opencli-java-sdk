package io.github.hiwepy.opencli.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 与 opencli-admin Agent {@code POST /collect} 请求体对齐的 DTO。
 * <p>
 * 参考 {@code backend/agent_server.py} 中 {@code CollectRequest}。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OpenCliCollectRequest {

    private String site;

    private String command;

    @Builder.Default
    private Map<String, Object> args = new LinkedHashMap<>();

    @JsonProperty("positional_args")
    @Builder.Default
    private List<String> positionalArgs = new ArrayList<>();

    @Builder.Default
    private String format = "json";

    @Builder.Default
    private String mode = "cdp";

    /**
     * 覆盖 Agent 侧默认 CDP；空串表示使用 Agent 环境变量。
     */
    @JsonProperty("cdp_endpoint")
    private String cdpEndpoint;
}
