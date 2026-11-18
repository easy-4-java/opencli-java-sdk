package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight channel for one OpenCLI adapter. Values are literal argv tokens;
 * only the separately supplied adapter identifier is normalized.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Slf4j
public final class OpenCliAdapterChannel {

    private final OpenCliExecutor executor;
    private final String adapterId;

    /**
     * @param executor shared executor
     * @param adapterId nonblank adapter identifier
     */
    public OpenCliAdapterChannel(OpenCliExecutor executor, String adapterId) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.adapterId = Objects.requireNonNull(adapterId, "adapterId").trim();
        if (OpenCliStrings.isBlank(this.adapterId)) {
            throw new IllegalArgumentException("adapterId must not be blank");
        }
    }

    /** @return this channel's adapter identifier */
    public String getAdapterId() {
        return adapterId;
    }

    /**
     * Invoke an adapter with a snapshot of the supplied literal arguments.
     * An empty list invokes its root; null elements are rejected.
     *
     * @param subcommandAndArgs subcommand and subsequent values
     * @return execution result
     */
    public OpenCliResult invoke(List<String> subcommandAndArgs) {
        List<String> rest = OpenCliArgSupport.snapshotValues(subcommandAndArgs, "subcommandAndArgs");
        List<String> tokens = new ArrayList<>(rest.size() + 1);
        tokens.add(adapterId);
        tokens.addAll(rest);
        // Do not put prompt text or positional values in default diagnostic logs.
        log.debug("OpenCLI adapter invoke argvSize={}", tokens.size());
        return executor.invoke(tokens);
    }

    /**
     * @param request structured command request
     * @return execution result
     */
    public OpenCliResult invoke(OpenCliAdapterCommandRequest request) {
        Objects.requireNonNull(request, "request");
        return invoke(request.toSubcommandAndArgs());
    }

    /**
     * @param subcommandAndArgs subcommand and literal values
     * @return execution result
     */
    public OpenCliResult invoke(String... subcommandAndArgs) {
        Objects.requireNonNull(subcommandAndArgs, "subcommandAndArgs");
        return invoke(Arrays.asList(subcommandAndArgs));
    }
}
