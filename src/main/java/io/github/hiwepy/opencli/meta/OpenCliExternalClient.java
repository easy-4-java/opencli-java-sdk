package io.github.hiwepy.opencli.meta;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli external *} 与已注册外部 CLI 的透传入口。
 */
@RequiredArgsConstructor
public final class OpenCliExternalClient {

    private final OpenCliExecutor executor;

    private OpenCliResult invokeExternalSub(List<String> rest) {
        List<String> tokens = new ArrayList<>();
        tokens.add("external");
        tokens.addAll(rest);
        return executor.invoke(tokens);
    }

    /** {@code external install <name>} */
    public OpenCliResult install(String name) {
        return invokeExternalSub(OpenCliLists.of("install", name));
    }

    /**
     * {@code external register <name>}。
     */
    public OpenCliResult register(String name, String binary, String installCmd, String description) {
        List<String> args = new ArrayList<>();
        args.add("register");
        args.add(name);
        if (binary != null) {
            OpenCliArgSupport.addOptionPair(args, "--binary", binary);
        }
        if (installCmd != null) {
            OpenCliArgSupport.addOptionPair(args, "--install", installCmd);
        }
        if (description != null) {
            OpenCliArgSupport.addOptionPair(args, "--desc", description);
        }
        return invokeExternalSub(args);
    }

    /** {@code external list} */
    public OpenCliResult list(String format) {
        List<String> args = new ArrayList<>();
        args.add("list");
        if (format != null) {
            args.add("-f");
            args.add(format);
        }
        return invokeExternalSub(args);
    }

    /** @see #list(String) */
    public OpenCliResult list() {
        return list(null);
    }

    /**
     * 透传调用已注册的外部 CLI：{@code opencli <name> [args...]}。
     *
     * @param externalCliName 外部 CLI 名（根级命令）
     * @param passthroughArgs 传给外部二进制的参数
     */
    public OpenCliResult passthrough(String externalCliName, List<String> passthroughArgs) {
        List<String> tokens = new ArrayList<>();
        tokens.add(externalCliName);
        if (passthroughArgs != null) {
            tokens.addAll(passthroughArgs);
        }
        return executor.invoke(tokens);
    }
}
