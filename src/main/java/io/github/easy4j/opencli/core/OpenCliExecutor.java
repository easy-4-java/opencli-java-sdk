package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliTimeoutException;
import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import io.github.easy4j.opencli.remote.OpenCliArgvToCollectParser;
import io.github.easy4j.opencli.remote.OpenCliCollectRequest;
import io.github.easy4j.opencli.remote.OpenCliRemoteAgentHttpClient;
import io.github.easy4j.opencli.core.support.SubprocessExecutionSupport;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;

/**
 * OpenCLI subprocess execution wrapper based on Apache Commons Exec.
 *
 * <p>{@link #invoke(List)} accepts literal argv tokens following the executable name.
 * Local invocation prepends {@link OpenCliProperties#getLeadingArguments()}.
 * Empty and whitespace-only values are preserved; null tokens are rejected before
 * transport selection. Command identifiers are validated separately.</p>
 *
 * <p>When the execution target is {@link OpenCliExecutionTarget#REMOTE_AGENT_HTTP},
 * invocation uses the legacy opencli-admin {@code /collect} contract. That protocol's
 * representation limits are separate from the local raw argv contract.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Slf4j
@Getter
public class OpenCliExecutor {

    private final OpenCliProperties properties;

    /** Lazily initialized for remote mode. */
    private volatile OpenCliRemoteAgentHttpClient remoteAgentHttpClient;

    /** @param properties runtime configuration */
    public OpenCliExecutor(OpenCliProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        SubprocessExecutionSupport.configureMaxConcurrentExecutions(properties.getMaxConcurrentExecutions());
    }

    /**
     * Invoke a snapshot of the complete argv vector, excluding the executable.
     *
     * @param adapterAndRest nonempty command and literal values
     * @return execution result
     */
    public OpenCliResult invoke(List<String> adapterAndRest) {
        List<String> tokens = OpenCliArgSupport.snapshotValues(adapterAndRest, "adapterAndRest");
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("adapterAndRest must contain at least the command identifier");
        }
        if (OpenCliStrings.isBlank(tokens.get(0))) {
            throw new IllegalArgumentException("adapterAndRest[0] command identifier must not be blank");
        }
        if (properties.getExecutionTarget() == OpenCliExecutionTarget.REMOTE_AGENT_HTTP) {
            log.debug("OpenCLI invoke remote agent argvSize={}", tokens.size());
            OpenCliCollectRequest req =
                OpenCliArgvToCollectParser.parse(
                    tokens,
                    properties.getRemoteOutputFormat(),
                    properties.getRemoteCollectMode(),
                    properties.getRemoteCdpEndpoint());
            return remoteAgent().collect(req);
        }
        log.debug("OpenCLI invoke local argvSize={}", tokens.size());
        CommandLine cmd = buildCommandLine(tokens);
        return run(cmd);
    }

    private OpenCliRemoteAgentHttpClient remoteAgent() {
        if (Objects.isNull(remoteAgentHttpClient)) {
            synchronized (this) {
                if (Objects.isNull(remoteAgentHttpClient)) {
                    remoteAgentHttpClient = new OpenCliRemoteAgentHttpClient(properties);
                }
            }
        }
        return remoteAgentHttpClient;
    }

    /**
     * @param adapterAndRest command and literal values
     * @return execution result
     */
    public OpenCliResult invoke(String... adapterAndRest) {
        Objects.requireNonNull(adapterAndRest, "adapterAndRest");
        return invoke(Arrays.asList(adapterAndRest));
    }

    private CommandLine buildCommandLine(List<String> adapterAndRest) {
        String exe = properties.getExecutable();
        if (OpenCliStrings.isBlank(exe)) {
            throw new IllegalStateException("opencli.executable must not be blank");
        }
        CommandLine cmd = new LiteralCommandLine(exe.trim());
        appendLiteralArgs(cmd, properties.getLeadingArguments(), "leadingArguments");
        appendLiteralArgs(cmd, adapterAndRest, "adapterAndRest");
        return cmd;
    }

    private static void appendLiteralArgs(CommandLine cmd, List<String> args, String field) {
        if (args == null) {
            return;
        }
        for (String value : OpenCliArgSupport.snapshotValues(args, field)) {
            cmd.addArgument(value, false);
        }
    }

    /**
     * Append a {@code --key=value} token using Commons Exec quoting.
     *
     * @param cmd command line
     * @param key option name starting with {@code --}
     * @param value non-null value
     */
    public static void appendQuotedKeyValue(CommandLine cmd, String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.startsWith("--")) {
            throw new IllegalArgumentException("CLI key must start with '--', got: " + key);
        }
        String prefix = key.endsWith("=") ? key.substring(0, key.length() - 1) : key;
        cmd.addArgument(prefix + "=" + value, true);
    }

    private OpenCliResult run(CommandLine commandLine) {
        long timeoutMs = properties.getCommandTimeoutMillis();
        if (timeoutMs <= 0) {
            throw new IllegalStateException("opencli.command-timeout-millis must be positive");
        }

        File workingDirectory = resolveWorkingDirectory();
        Map<String, String> environment = buildEnvironment();
        SubprocessExecutionSupport.ExecutionRequest request =
                new SubprocessExecutionSupport.ExecutionRequest(
                        commandLine, workingDirectory, environment, timeoutMs);

        try {
            SubprocessExecutionSupport.RunSession session = SubprocessExecutionSupport.execute(request);
            return completeAfterWait(
                    commandLine,
                    timeoutMs,
                    session.getStdout(),
                    session.getStderr(),
                    session.getHandler(),
                    session.getWatchdog(),
                    session.isWaitTimedOut());
        } catch (IOException e) {
            log.warn("OpenCLI spawn failed commandLine={}, message={}", commandLine, e.getMessage());
            throw new OpenCliExecutableFailureException(
                    "OpenCLI could not be started (check PATH or executable path): " + commandLine, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OpenCLI interrupted commandLine={}", commandLine);
            throw new OpenCliException("Interrupted while awaiting OpenCLI subprocess", e, null);
        }
    }

    private File resolveWorkingDirectory() {
        String wdProperty = properties.getWorkingDirectory();
        if (OpenCliStrings.isNotBlank(wdProperty)) {
            File wd = new File(wdProperty.trim());
            if (!wd.isDirectory()) {
                throw new OpenCliExecutableFailureException(
                        "opencli.working-directory is not an existing directory: " + wd.getAbsolutePath(), null);
            }
            return wd;
        }
        return null;
    }

    private OpenCliResult completeAfterWait(
            CommandLine commandLine,
            long timeoutMs,
            ByteArrayOutputStream out,
            ByteArrayOutputStream err,
            DefaultExecuteResultHandler handler,
            ExecuteWatchdog watchdog,
            boolean waitTimedOut) {
        String stdoutStr = new String(out.toByteArray(), StandardCharsets.UTF_8);
        String stderrStr = new String(err.toByteArray(), StandardCharsets.UTF_8);
        OpenCliParsedFields parsed = OpenCliOutputParser.parseBestEffort(stdoutStr, stderrStr);

        if (waitTimedOut || watchdog.killedProcess()) {
            log.warn("OpenCLI timed out commandLine={} timeoutMs={}", commandLine, timeoutMs);
            OpenCliResult partial = snapshot(stdoutStr, stderrStr, readExitQuietly(handler), parsed);
            throw new OpenCliTimeoutException(
                    "OpenCLI timed out after " + timeoutMs + " ms: " + commandLine, partial);
        }

        Exception asyncFailure = handler.getException();
        if (asyncFailure instanceof ExecuteException) {
            ExecuteException ex = (ExecuteException) asyncFailure;
            log.warn("OpenCLI failed exitCode={} commandLine={}", ex.getExitValue(), commandLine);
            OpenCliResult failed = snapshot(stdoutStr, stderrStr, normalizeExitValue(ex.getExitValue()), parsed);
            throw new OpenCliNonZeroExitException(
                    "OpenCLI failed (exitCode=" + ex.getExitValue() + "): " + commandLine, failed);
        }
        if (Objects.nonNull(asyncFailure)) {
            log.error("OpenCLI async failure commandLine={}", commandLine, asyncFailure);
            OpenCliResult snapshot = snapshot(stdoutStr, stderrStr, readExitQuietly(handler), parsed);
            throw new OpenCliException(
                    "OpenCLI async failure: " + commandLine + " cause=" + asyncFailure.getMessage(),
                    asyncFailure, snapshot);
        }

        final int exit;
        try {
            exit = handler.getExitValue();
        } catch (IllegalStateException e) {
            throw new OpenCliException(
                    "OpenCLI completed without observable exit code: " + commandLine,
                    e,
                    snapshot(stdoutStr, stderrStr, null, parsed));
        }

        if (exit != 0) {
            log.warn("OpenCLI non-zero exit exitCode={} commandLine={}", exit, commandLine);
            OpenCliResult failed = snapshot(stdoutStr, stderrStr, exit, parsed);
            throw new OpenCliNonZeroExitException(
                    "OpenCLI non-zero exit (exitCode=" + exit + "): " + commandLine, failed);
        }

        return OpenCliResult.builder()
                .stdout(stdoutStr)
                .stderr(stderrStr)
                .exitCode(exit)
                .success(true)
                .parsed(parsed)
                .build();
    }

    private Map<String, String> buildEnvironment() {
        Map<String, String> env = new HashMap<>(System.getenv());
        if (Objects.nonNull(properties.getEnvironment())) {
            for (Map.Entry<String, String> e : properties.getEnvironment().entrySet()) {
                if (Objects.nonNull(e.getKey()) && Objects.nonNull(e.getValue())) {
                    env.put(e.getKey(), e.getValue());
                }
            }
        }
        return env;
    }

    private static Integer readExitQuietly(DefaultExecuteResultHandler handler) {
        try {
            return normalizeExitValue(handler.getExitValue());
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static Integer normalizeExitValue(int raw) {
        if (raw == org.apache.commons.exec.Executor.INVALID_EXITVALUE) {
            return null;
        }
        return raw;
    }

    private static OpenCliResult snapshot(
        String stdoutStr, String stderrStr, Integer exitCode, OpenCliParsedFields parsed) {
        return OpenCliResult.builder()
            .stdout(Objects.isNull(stdoutStr) ? "" : stdoutStr)
            .stderr(Objects.isNull(stderrStr) ? "" : stderrStr)
            .exitCode(exitCode)
            .success(false)
            .parsed(parsed)
            .build();
    }
}
