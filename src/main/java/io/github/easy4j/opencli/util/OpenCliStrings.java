package io.github.easy4j.opencli.util;

/**
 * String utilities, avoiding hard dependency on JDK 11+ {@link String#isBlank()}
 * and consistent with the existing starter utility style.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */public final class OpenCliStrings {

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
