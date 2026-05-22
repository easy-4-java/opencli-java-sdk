package io.github.hiwepy.opencli.core;

import io.github.hiwepy.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CLI 参数拼装辅助：合并业务片段与透传 {@code additionalRawArgs}。
 */
public final class OpenCliArgSupport {

    private OpenCliArgSupport() {
    }

    /**
     * 将前缀片段与可选附加片段合并为连续 argv（过滤 null/空白）。
     *
     * @param prefix           子命令与已建模参数，可为 null
     * @param additionalRawArgs 额外原生参数，可为 null
     * @return 新列表副本
     */
    public static List<String> merge(List<String> prefix, List<String> additionalRawArgs) {
        List<String> out = new ArrayList<>();
        if (Objects.nonNull(prefix)) {
            for (String s : prefix) {
                if (OpenCliStrings.isNotBlank(s)) {
                    out.add(s.trim());
                }
            }
        }
        if (Objects.nonNull(additionalRawArgs)) {
            for (String s : additionalRawArgs) {
                if (OpenCliStrings.isNotBlank(s)) {
                    out.add(s.trim());
                }
            }
        }
        return out;
    }

    /**
     * 追加 {@code --name=value}（value 含空格时由调用方决定是否使用
     * {@link OpenCliExecutor#appendQuotedKeyValue(CommandLine, String, String)}；
     * 此处仅做简单拼接）。
     *
     * @param target 目标列表，不得为 null
     * @param name   完整名称（含 {@code --}，不含 {@code =}）
     * @param value  非空值
     */
    public static void addOptionEquals(List<String> target, String name, String value) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (!name.startsWith("--")) {
            throw new IllegalArgumentException("name must start with '--', got: " + name);
        }
        String key = name.endsWith("=") ? name.substring(0, name.length() - 1) : name;
        target.add(key + "=" + value);
    }

    /**
     * 追加 {@code --flag value} 双 token 形式。
     *
     * @param target 目标列表
     * @param flag   如 {@code --limit}
     * @param value  非空值
     */
    public static void addOptionPair(List<String> target, String flag, String value) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(flag, "flag");
        Objects.requireNonNull(value, "value");
        target.add(flag);
        target.add(value);
    }

    /**
     * 当 {@code value} 非 null 时追加 {@code --flag value}。
     *
     * @param target 目标 argv 列表
     * @param flag   选项名
     * @param value  可为 null
     */
    public static void addOptionPairIfPresent(List<String> target, String flag, Object value) {
        if (Objects.nonNull(value)) {
            addOptionPair(target, flag, String.valueOf(value));
        }
    }

    /**
     * 当 {@code enabled} 为 {@code true} 时追加 boolean flag（无值）。
     *
     * @param target  目标 argv 列表
     * @param flag    如 {@code --follow}
     * @param enabled 开关，null/false 时不追加
     */
    public static void addFlagIfTrue(List<String> target, String flag, Boolean enabled) {
        if (Boolean.TRUE.equals(enabled)) {
            Objects.requireNonNull(target, "target").add(flag);
        }
    }
}
