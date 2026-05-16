package io.github.hiwepy.opencli.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.core.OpenCliTypedResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 将 {@code -f json} 等场景下的 stdout 解析为 Jackson {@link JsonNode}。
 */
@Slf4j
public final class OpenCliStdoutJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OpenCliStdoutJson() {
    }

    /**
     * 尝试将 stdout 解析为 JSON；失败时返回 text 节点包装原文（避免强依赖具体 CLI 版本）。
     *
     * @param stdout 标准输出全文
     * @return 非 null 的 {@link JsonNode}
     */
    public static JsonNode parseLenient(String stdout) {
        if (stdout == null || stdout.isEmpty()) {
            return MAPPER.getNodeFactory().textNode("");
        }
        String trim = stdout.trim();
        try {
            return MAPPER.readTree(trim);
        } catch (JsonProcessingException e) {
            log.debug("stdout is not strict JSON, wrapping as text: {}", e.getMessage());
            return MAPPER.getNodeFactory().textNode(stdout);
        }
    }

    /**
     * 为成功的 {@link OpenCliResult} 绑定解析后的 JSON 视图。
     *
     * @param raw 原始结果，不得为 null
     * @return typed 包装
     */
    public static OpenCliTypedResult<JsonNode> typed(OpenCliResult raw) {
        JsonNode node = parseLenient(raw.getStdout());
        return OpenCliTypedResult.of(raw, node);
    }
}
