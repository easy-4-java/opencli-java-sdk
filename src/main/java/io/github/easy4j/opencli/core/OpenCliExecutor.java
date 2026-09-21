package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import io.github.easy4j.opencli.core.support.SubprocessExecutionSupport;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliTimeoutException;
import io.github.easy4j.opencli.remote.OpenCliArgvToCollectParser;
import io.github.easy4j.opencli.remote.OpenCliCollectRequest;
import io.github.easy4j.opencli.remote.OpenCliRemoteAgentHttpClient;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.exec.CommandLine;

/**
 * Literal-argv executor with a stable capacity owner, total local deadline and
 * bounded output. Existing synchronous entry points are retained. Raw HTTP
 * collect remains a separate legacy protocol, not a lossless process transport.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Getter
public class OpenCliExecutor {
    private final OpenCliProperties properties;
    private final OpenCliProcessRuntime processRuntime;
    private volatile OpenCliRemoteAgentHttpClient remoteAgentHttpClient;

    /** @param properties configuration; process capacity is captured once */
    public OpenCliExecutor(OpenCliProperties properties) {
        this(properties, new OpenCliProcessRuntime(Objects.requireNonNull(properties, "properties")
            .getMaxConcurrentExecutions()));
    }

    /**
     * @param properties configuration
     * @param processRuntime an explicitly shared, stable local capacity owner
     */
    public OpenCliExecutor(OpenCliProperties properties, OpenCliProcessRuntime processRuntime) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.processRuntime = Objects.requireNonNull(processRuntime, "processRuntime");
    }

    /** @param adapterAndRest command and literal values @return execution result */
    public OpenCliResult invoke(List<String> adapterAndRest) {
        return invokeInternal(adapterAndRest, new OpenCliCancellationToken(), false);
    }

    /**
     * Cancellable local invocation. The legacy HTTP protocol does not claim a
     * cancellable remote process; explicit tokens are rejected in remote mode.
     *
     * @param adapterAndRest command and literal values
     * @param cancellationToken cancellation for this invocation only
     * @return execution result
     */
    public OpenCliResult invoke(List<String> adapterAndRest, OpenCliCancellationToken cancellationToken) {
        return invokeInternal(adapterAndRest, Objects.requireNonNull(cancellationToken, "cancellationToken"), true);
    }

    private OpenCliResult invokeInternal(List<String> adapterAndRest,
        OpenCliCancellationToken cancellationToken, boolean explicitCancellation) {
        long submittedAtNanos = System.nanoTime();
        long timeoutMillis = properties.getCommandTimeoutMillis();
        List<String> tokens = OpenCliArgSupport.snapshotValues(adapterAndRest, "adapterAndRest");
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("adapterAndRest must contain at least the command identifier");
        }
        if (OpenCliStrings.isBlank(tokens.get(0))) {
            throw new IllegalArgumentException("adapterAndRest[0] command identifier must not be blank");
        }
        if (properties.getExecutionTarget() == OpenCliExecutionTarget.REMOTE_AGENT_HTTP) {
            if (explicitCancellation) {
                throw new UnsupportedOperationException("Explicit process cancellation is local-only for legacy collect");
            }
            OpenCliCollectRequest req = OpenCliArgvToCollectParser.parse(tokens,
                properties.getRemoteOutputFormat(), properties.getRemoteCollectMode(), properties.getRemoteCdpEndpoint());
            return remoteAgent().collect(req);
        }
        CommandLine commandLine = buildCommandLine(tokens);
        return run(commandLine, timeoutMillis, submittedAtNanos, cancellationToken);
    }

    private OpenCliRemoteAgentHttpClient remoteAgent() {
        if (remoteAgentHttpClient == null) {
            synchronized (this) {
                if (remoteAgentHttpClient == null) {
                    remoteAgentHttpClient = new OpenCliRemoteAgentHttpClient(properties);
                }
            }
        }
        return remoteAgentHttpClient;
    }

    /** @param adapterAndRest command and literal values @return execution result */
    public OpenCliResult invoke(String... adapterAndRest) {
        Objects.requireNonNull(adapterAndRest, "adapterAndRest");
        return invoke(Arrays.asList(adapterAndRest));
    }

    private CommandLine buildCommandLine(List<String> tokens) {
        String executable = properties.getExecutable();
        if (OpenCliStrings.isBlank(executable)) {
            throw new IllegalStateException("opencli.executable must not be blank");
        }
        String normalized = executable.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (System.getProperty("os.name").startsWith("Windows")
            && (lower.endsWith(".cmd") || lower.endsWith(".bat"))) {
            throw new UnsupportedOperationException("Use a native node executable plus the CLI JavaScript path; batch shims are not literal argv transports");
        }
        CommandLine cmd = new LiteralCommandLine(normalized);
        appendLiteralArgs(cmd, properties.getLeadingArguments(), "leadingArguments");
        appendLiteralArgs(cmd, tokens, "adapterAndRest");
        return cmd;
    }

    private static void appendLiteralArgs(CommandLine cmd, List<String> values, String field) {
        if (values == null) { return; }
        for (String value : OpenCliArgSupport.snapshotValues(values, field)) { cmd.addArgument(value, false); }
    }

    /** Legacy explicit-quoting helper; not used by the literal process path. */
    public static void appendQuotedKeyValue(CommandLine cmd, String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.startsWith("--")) {
            throw new IllegalArgumentException("CLI key must start with '--'");
        }
        String prefix = key.endsWith("=") ? key.substring(0, key.length() - 1) : key;
        cmd.addArgument(prefix + "=" + value, true);
    }

    private OpenCliResult run(CommandLine commandLine, long timeoutMillis, long submittedAtNanos,
        OpenCliCancellationToken cancellationToken) {
        if (timeoutMillis <= 0) {
            throw new IllegalStateException("opencli.command-timeout-millis must be positive");
        }
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            commandLine, resolveWorkingDirectory(), buildEnvironment(), timeoutMillis,
            properties.getMaxStdoutBytes(), properties.getMaxStderrBytes(), properties.getCleanupGraceMillis(),
            submittedAtNanos, cancellationToken);
        try {
            return complete(processRuntime.execute(request));
        } catch (IOException ex) {
            throw new OpenCliExecutableFailureException("OpenCLI process could not be started", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OpenCliException("Interrupted while awaiting OpenCLI subprocess", ex, null);
        }
    }

    private OpenCliResult complete(SubprocessExecutionSupport.RunSession session) {
        String stdout = new String(session.getStdout().toByteArray(), StandardCharsets.UTF_8);
        String stderr = new String(session.getStderr().toByteArray(), StandardCharsets.UTF_8);
        OpenCliExecutionDetails details = session.getExecutionDetails();
        TerminationReason reason = details.getTerminationReason();
        Integer exit = session.getObservedExitCode();
        boolean success = reason == TerminationReason.PROCESS_EXIT && Integer.valueOf(0).equals(exit)
            && details.isStreamsDrained();
        OpenCliResult result = OpenCliResult.builder().stdout(stdout).stderr(stderr).exitCode(exit)
            .success(success).parsed(OpenCliOutputParser.parseBestEffort(stdout, stderr))
            .executionDetails(details).build();
        if (success) { return result; }
        if (reason == TerminationReason.QUEUE_TIMEOUT || reason == TerminationReason.EXECUTION_TIMEOUT) {
            throw new OpenCliTimeoutException("OpenCLI deadline exceeded: " + reason, result);
        }
        if (reason == TerminationReason.SPAWN_FAILED) {
            throw new OpenCliExecutableFailureException("OpenCLI process could not be started", session.getIoFailure(), result);
        }
        if (reason == TerminationReason.PROCESS_EXIT && exit != null && exit != 0) {
            throw new OpenCliNonZeroExitException("OpenCLI returned nonzero exitCode=" + exit, result);
        }
        throw new OpenCliException("OpenCLI execution ended: " + reason, session.getIoFailure(), result);
    }

    private File resolveWorkingDirectory() {
        String value = properties.getWorkingDirectory();
        if (OpenCliStrings.isNotBlank(value)) {
            File directory = new File(value.trim());
            if (!directory.isDirectory()) {
                throw new OpenCliExecutableFailureException("opencli.working-directory is not an existing directory", null);
            }
            return directory;
        }
        return null;
    }

    private Map<String, String> buildEnvironment() {
        Map<String, String> env = new HashMap<>(System.getenv());
        if (properties.getEnvironment() != null) {
            for (Map.Entry<String, String> entry : new HashMap<>(properties.getEnvironment()).entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) { env.put(entry.getKey(), entry.getValue()); }
            }
        }
        return env;
    }
}
