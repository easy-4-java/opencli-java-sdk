package io.github.hiwepy.opencli.core;

import io.github.hiwepy.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 针对单个 OpenCLI adapter 的轻量通道：自动在 argv 前插入 adapter id。
 */
public final class OpenCliAdapterChannel {

    private final OpenCliExecutor executor;
    private final String adapterId;

    /**
     * @param executor   共享执行器，不得为 null
     * @param adapterId  文档中的 adapter 名（如 {@code twitter}），不得为空白
     */
    public OpenCliAdapterChannel(OpenCliExecutor executor, String adapterId) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.adapterId = Objects.requireNonNull(adapterId, "adapterId").trim();
        if (OpenCliStrings.isBlank(this.adapterId)) {
            throw new IllegalArgumentException("adapterId must not be blank");
        }
    }

    /**
     * @return 当前通道绑定的 adapter id
     */
    public String getAdapterId() {
        return adapterId;
    }

    /**
     * 调用 {@code opencli <adapter> <subcommandAndArgs...>}。
     *
     * @param subcommandAndArgs 子命令及后续参数；不得为 null，可为空（仅调 adapter 根命令时）
     * @return 成功时的 {@link OpenCliResult}
     */
    public OpenCliResult invoke(List<String> subcommandAndArgs) {
        Objects.requireNonNull(subcommandAndArgs, "subcommandAndArgs");
        List<String> tokens = new ArrayList<>();
        tokens.add(adapterId);
        for (String s : subcommandAndArgs) {
            if (OpenCliStrings.isNotBlank(s)) {
                tokens.add(s.trim());
            }
        }
        return executor.invoke(tokens);
    }

    /**
     * 通过 {@link OpenCliAdapterCommandRequest} 发起调用（推荐测试与 SDK 侧结构化入口）。
     *
     * @param request 结构化子命令请求，不得为 null
     * @return 执行结果
     */
    public OpenCliResult invoke(OpenCliAdapterCommandRequest request) {
        Objects.requireNonNull(request, "request");
        return invoke(request.toSubcommandAndArgs());
    }

    /**
     * {@link #invoke(List)} 的可变参数形式。
     *
     * @param subcommandAndArgs 子命令及 flag
     * @return 执行结果
     */
    public OpenCliResult invoke(String... subcommandAndArgs) {
        List<String> list = new ArrayList<>();
        if (subcommandAndArgs != null) {
            for (String s : subcommandAndArgs) {
                if (OpenCliStrings.isNotBlank(s)) {
                    list.add(s.trim());
                }
            }
        }
        return invoke(list);
    }
}
