package io.github.easy4j.opencli.meta;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli plugin *} 子命令封装。
 */
@RequiredArgsConstructor/**

 * Client for {@code opencli plugin *} subcommands.

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public final class OpenCliPluginClient {

    private final OpenCliExecutor executor;

    private OpenCliResult invoke(List<String> rest) {
        List<String> tokens = new ArrayList<>();
        tokens.add("plugin");
        tokens.addAll(rest);
        return executor.invoke(tokens);
    }

    /** {@code plugin install <source>} */
    public OpenCliResult install(String source) {
        return invoke(OpenCliLists.of("install", source));
    }

    /** {@code plugin uninstall <name>} */
    public OpenCliResult uninstall(String name) {
        return invoke(OpenCliLists.of("uninstall", name));
    }

    /** {@code plugin update <name>} */
    public OpenCliResult update(String name) {
        return invoke(OpenCliLists.of("update", name));
    }

    /** {@code plugin update --all} */
    public OpenCliResult updateAll() {
        return invoke(OpenCliLists.of("update", "--all"));
    }

    /** {@code plugin list} */
    public OpenCliResult list(String format) {
        List<String> args = new ArrayList<>();
        args.add("list");
        if (format != null) {
            args.add("-f");
            args.add(format);
        }
        return invoke(args);
    }

    /** @see #list(String) */
    public OpenCliResult list() {
        return list(null);
    }

    /**
     * {@code plugin create <name>}。
     */
    public OpenCliResult create(String name, String dir, String description) {
        List<String> args = new ArrayList<>();
        args.add("create");
        args.add(name);
        if (dir != null) {
            OpenCliArgSupport.addOptionPair(args, "--dir", dir);
        }
        if (description != null) {
            OpenCliArgSupport.addOptionPair(args, "--description", description);
        }
        return invoke(args);
    }
}
