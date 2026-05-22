package io.github.hiwepy.opencli.core;

import io.github.hiwepy.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * 结构化 adapter 子命令请求：由子命令名、positional 参数与命名 options 构建 argv，
 * 供 {@link OpenCliAdapterChannel#invoke(OpenCliAdapterCommandRequest)} 及覆盖测试使用。
 * <p>
 * 禁止在测试中手工拼接 {@code List.of("sub", "--flag", "value")}；应通过 builder 建模参数。
 * </p>
 */
@Getter
@Builder
public final class OpenCliAdapterCommandRequest {

    /** 子命令名（不含 adapter id）。 */
    private final String subcommand;

    @Getter(AccessLevel.NONE)
    @Singular("positional")
    private final List<String> positionals;

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private final Map<String, Object> options = Collections.emptyMap();

    /**
     * @return positional 参数副本
     */
    public List<String> getPositionals() {
        if (Objects.isNull(positionals)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(positionals));
    }

    /**
     * @return 命名选项副本
     */
    public Map<String, Object> getOptions() {
        if (Objects.isNull(options)) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }

    /**
     * 将本请求转换为 {@link OpenCliAdapterChannel#invoke(List)} 所需的 token 列表。
     *
     * @return 以 subcommand 开头、随后 positional、再 options 的 argv 片段
     */
    public List<String> toSubcommandAndArgs() {
        Objects.requireNonNull(subcommand, "subcommand");
        List<String> tokens = new ArrayList<>();
        tokens.add(subcommand.trim());
        if (Objects.nonNull(positionals)) {
            for (String p : positionals) {
                if (OpenCliStrings.isNotBlank(p)) {
                    tokens.add(p.trim());
                }
            }
        }
        if (Objects.nonNull(options)) {
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                appendOption(tokens, entry.getKey(), entry.getValue());
            }
        }
        return tokens;
    }

    private static void appendOption(List<String> target, String name, Object value) {
        if (OpenCliStrings.isBlank(name) || Objects.isNull(value)) {
            return;
        }
        String flag = name.startsWith("-") ? name.trim() : "--" + name.trim();
        if (value instanceof Boolean) {
            if (((Boolean) value).booleanValue()) {
                target.add(flag);
            }
            return;
        }
        target.add(flag);
        target.add(String.valueOf(value).trim());
    }

    /**
     * 从 manifest 风格的 options map 创建请求（测试资源反序列化辅助）。
     *
     * @param subcommand 子命令
     * @param positionals positional 列表，可为 null
     * @param options 选项 map，可为 null
     * @return 请求实例
     */
    public static OpenCliAdapterCommandRequest of(
        String subcommand,
        List<String> positionals,
        Map<String, Object> options) {
        OpenCliAdapterCommandRequestBuilder b = builder().subcommand(subcommand);
        if (Objects.nonNull(positionals)) {
            b.positionals(positionals);
        }
        if (Objects.nonNull(options) && !options.isEmpty()) {
            b.options(new LinkedHashMap<>(options));
        }
        return b.build();
    }
}
