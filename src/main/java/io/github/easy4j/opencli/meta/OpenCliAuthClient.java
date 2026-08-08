package io.github.easy4j.opencli.meta;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli auth *} 子命令封装：登录状态查询与会话刷新。
 * <p>支持 {@code auth status} 与 {@code auth refresh}。</p>
 */
@RequiredArgsConstructor/**

 * Client for {@code opencli auth *} subcommands: login status queries and session refresh.
 *
 * <p>Supports {@code auth status} and {@code auth refresh}.</p>

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public final class OpenCliAuthClient {

    private final OpenCliExecutor executor;

    private OpenCliResult invokeAuthSub(List<String> rest) {
        List<String> tokens = new ArrayList<>();
        tokens.add("auth");
        tokens.addAll(rest);
        return executor.invoke(tokens);
    }

    /**
     * {@code auth status [--site ...] [--full] [--concurrency N] [--timeout N] [--only status] [-f fmt]}。
     *
     * @param options 状态查询参数；可为 {@code null}
     */
    public OpenCliResult status(OpenCliAuthStatusOptions options) {
        List<String> args = new ArrayList<>();
        args.add("status");
        if (options != null) {
            OpenCliArgSupport.addOptionPairIfPresent(args, "--site", options.getSite());
            OpenCliArgSupport.addFlagIfTrue(args, "--full", options.getFull());
            OpenCliArgSupport.addOptionPairIfPresent(args, "--concurrency", options.getConcurrency());
            OpenCliArgSupport.addOptionPairIfPresent(args, "--timeout", options.getTimeout());
            OpenCliArgSupport.addOptionPairIfPresent(args, "--only", options.getOnly());
            if (Objects.nonNull(options.getFormat())) {
                args.add("-f");
                args.add(options.getFormat());
            }
        }
        return invokeAuthSub(args);
    }

    /** @see #status(OpenCliAuthStatusOptions) */
    public OpenCliResult status() {
        return status(null);
    }

    /**
     * {@code auth refresh [--site ... | --all] [--concurrency N] [--timeout N] [-f fmt]}。
     *
     * @param options 刷新参数；可为 {@code null}
     */
    public OpenCliResult refresh(OpenCliAuthRefreshOptions options) {
        List<String> args = new ArrayList<>();
        args.add("refresh");
        if (options != null) {
            OpenCliArgSupport.addOptionPairIfPresent(args, "--site", options.getSite());
            if (Boolean.TRUE.equals(options.getAll())) {
                args.add("--all");
            }
            OpenCliArgSupport.addOptionPairIfPresent(args, "--concurrency", options.getConcurrency());
            OpenCliArgSupport.addOptionPairIfPresent(args, "--timeout", options.getTimeout());
            if (Objects.nonNull(options.getFormat())) {
                args.add("-f");
                args.add(options.getFormat());
            }
        }
        return invokeAuthSub(args);
    }

    /** @see #refresh(OpenCliAuthRefreshOptions) */
    public OpenCliResult refresh() {
        return refresh(null);
    }
}
