package io.github.easy4j.opencli.center.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses local opencli stdout into an {@code items} array similar to
 * Python {@code agent_server._parse_output}, for use in {@code result} WebSocket messages.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */public final class OpenCliWsCollectStdoutItems {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OpenCliWsCollectStdoutItems() {
    }

    /**
     * @param stdout 子进程标准输出全文
     * @param format 输出格式（当前对 {@code json} 做结构化解析，其余格式退化为单条 {@code content} 记录）
     * @return 非 null 列表（可能为空）
     */
    public static List<JsonNode> parseItems(String stdout, String format) {
        String fmt = OpenCliStrings.isBlank(format) ? "json" : format.trim().toLowerCase();
        if ("json".equals(fmt)) {
            return parseJsonItems(stdout);
        }
        ObjectNode row = MAPPER.createObjectNode();
        row.put("content", stdout == null ? "" : stdout);
        return Collections.singletonList(row);
    }

    /**
     * 自 stdout 中提取首个 JSON 对象或数组（与 Python 从首个 {@code {[} 起切片类似）。
     *
     * @param stdout 原始输出
     * @return JSON 对象列表
     */
    private static List<JsonNode> parseJsonItems(String stdout) {
        List<JsonNode> out = new ArrayList<>();
        if (stdout == null || stdout.isEmpty()) {
            return out;
        }
        int start = -1;
        for (int i = 0; i < stdout.length(); i++) {
            char c = stdout.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return out;
        }
        try {
            JsonNode root = MAPPER.readTree(stdout.substring(start));
            if (root.isArray()) {
                root.forEach(out::add);
            } else if (root.isObject()) {
                out.add(root);
            } else {
                out.add(root);
            }
        } catch (Exception ignored) {
            // 解析失败返回空列表，由上层将 success 置 false
        }
        return out;
    }
}
