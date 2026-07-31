package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import io.github.easy4j.opencli.util.OpenCliStrings;

/**
 * 对 stdout/stderr 做极轻量启发式解析。
 */
public final class OpenCliOutputParser {

    private OpenCliOutputParser() {
    }

    /**
     * 尽力识别 JSON 形态提示（不解析为 DOM，避免过大负载）。
     *
     * @param stdout 标准输出
     * @param stderr 标准错误
     * @return 非空 {@link OpenCliParsedFields}
     */
    public static OpenCliParsedFields parseBestEffort(String stdout, String stderr) {
        String sample = OpenCliStrings.isNotBlank(stdout) ? stdout.trim() : (stderr == null ? "" : stderr.trim());
        String hint = null;
        if (sample.startsWith("[") && sample.endsWith("]")) {
            hint = "array";
        } else if (sample.startsWith("{") && sample.endsWith("}")) {
            hint = "object";
        }
        return OpenCliParsedFields.builder().jsonShapeHint(hint).build();
    }
}
