package io.github.easy4j.opencli.util;

/**
 * 字符串工具，避免对 JDK 11+ {@link String#isBlank()} 的硬依赖表述歧义，并与既有 starter 工具风格对齐。
 */
public final class OpenCliStrings {

    private OpenCliStrings() {
    }

    /**
     * 判断是否为 null、空串或仅空白字符。
     *
     * @param value 待检测字符串，可为 null
     * @return 无有效内容时返回 true
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 与 {@link #isBlank(String)} 相反。
     *
     * @param value 待检测字符串，可为 null
     * @return 包含非空白字符时返回 true
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}
