package io.github.hiwepy.opencli.core;

import io.github.hiwepy.opencli.OpenCliExecutionTarget;
import io.github.hiwepy.opencli.OpenCliProperties;
import io.github.hiwepy.opencli.exception.OpenCliException;
import io.github.hiwepy.opencli.exception.OpenCliExecutableFailureException;
import io.github.hiwepy.opencli.exception.OpenCliNonZeroExitException;
import io.github.hiwepy.opencli.exception.OpenCliTimeoutException;
import io.github.hiwepy.opencli.parser.OpenCliParsedFields;
import io.github.hiwepy.opencli.remote.OpenCliArgvToCollectParser;
import io.github.hiwepy.opencli.remote.OpenCliCollectRequest;
import io.github.hiwepy.opencli.remote.OpenCliRemoteAgentHttpClient;
import io.github.hiwepy.opencli.util.OpenCliStrings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * 基于 Apache Commons Exec 的 OpenCLI 子进程执行封装。
 * <p>
 * {@link #invoke(List)} 接受的参数为「紧跟可执行名之后」的完整 token 列表，形如
 * {@code [adapter, subcommand, ...]}；本地模式下会自动拼接 {@link OpenCliProperties#getLeadingArguments()}。
 * </p>
 * <p>
 * 当 {@link OpenCliProperties#getExecutionTarget()} 为 {@link OpenCliExecutionTarget#REMOTE_AGENT_HTTP} 时，
 * 通过 {@link OpenCliRemoteAgentHttpClient} 调用远端 {@code POST /collect}；此时 {@code leadingArguments} 不参与请求，
 * argv 会被解析为 {@link OpenCliCollectRequest}（与 opencli-admin {@code agent_server} 契约一致）。
 * </p>
 */
@Slf4j
public class OpenCliExecutor {

    private final OpenCliProperties properties;

    /**
     * 懒加载，仅远程模式使用。
     */
    private volatile OpenCliRemoteAgentHttpClient remoteAgentHttpClient;

    /**
     * @param properties 运行时配置，不得为 null
     */
    public OpenCliExecutor(OpenCliProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * 执行 {@code opencli <adapter> ...} 完整 argv（不含可执行文件本身）。
     *
     * @param adapterAndRest 至少包含 adapter 名，后续为子命令与 flag；不得为 null
     * @return 成功时 {@link OpenCliResult#isSuccess()} 为 true
     */
    public OpenCliResult invoke(List<String> adapterAndRest) {
        Objects.requireNonNull(adapterAndRest, "adapterAndRest");
        if (properties.getExecutionTarget() == OpenCliExecutionTarget.REMOTE_AGENT_HTTP) {
            OpenCliCollectRequest req =
                OpenCliArgvToCollectParser.parse(
                    adapterAndRest,
                    properties.getRemoteOutputFormat(),
                    properties.getRemoteCollectMode(),
                    properties.getRemoteCdpEndpoint());
            return remoteAgent().collect(req);
        }
        CommandLine cmd = buildCommandLine(adapterAndRest);
        return run(cmd);
    }

    /**
     * @return 远程 Agent HTTP 客户端（懒加载）
     */
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

    /**
     * 便捷重载：可变参数形式。
     *
     * @param adapterAndRest adapter 及后续 CLI token
     * @return 执行结果
     */
    public OpenCliResult invoke(String... adapterAndRest) {
        List<String> list = new ArrayList<>();
        if (adapterAndRest != null) {
            for (String s : adapterAndRest) {
                if (OpenCliStrings.isNotBlank(s)) {
                    list.add(s.trim());
                }
            }
        }
        return invoke(list);
    }

    /**
     * 拼装 {@link CommandLine}：executable + leading + tokens。
     */
    private CommandLine buildCommandLine(List<String> adapterAndRest) {
        if (adapterAndRest.isEmpty()) {
            throw new IllegalArgumentException("adapterAndRest must contain at least the adapter id");
        }
        String exe = properties.getExecutable();
        if (OpenCliStrings.isBlank(exe)) {
            throw new IllegalStateException("opencli.executable must not be blank");
        }
        CommandLine cmd = new CommandLine(exe.trim());
        appendCleanArgs(cmd, properties.getLeadingArguments());
        appendCleanArgs(cmd, adapterAndRest);
        return cmd;
    }

    private static void appendCleanArgs(CommandLine cmd, List<String> args) {
        if (args == null || args.isEmpty()) {
            return;
        }
        for (String a : args) {
            if (OpenCliStrings.isNotBlank(a)) {
                cmd.addArgument(a.trim(), false);
            }
        }
    }

    /**
     * 将 {@code --key=value} 以句柄安全形式追加（含空格时由 Commons Exec 处理）。
     *
     * @param cmd   命令行
     * @param key   必须以 {@code --} 开头
     * @param value 非空值
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

    @SuppressWarnings("deprecation")
    private OpenCliResult run(CommandLine commandLine) {
        long timeoutMs = properties.getCommandTimeoutMillis();
        if (timeoutMs <= 0) {
            throw new IllegalStateException("opencli.command-timeout-millis must be positive");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(out, err));

        String wdProperty = properties.getWorkingDirectory();
        if (OpenCliStrings.isNotBlank(wdProperty)) {
            File wd = new File(wdProperty.trim());
            if (!wd.isDirectory()) {
                throw new OpenCliExecutableFailureException(
                    "opencli.working-directory is not an existing directory: " + wd.getAbsolutePath(), null);
            }
            executor.setWorkingDirectory(wd);
        }

        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMs);
        executor.setWatchdog(watchdog);
        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();

        Map<String, String> environment = buildEnvironment();

        try {
            executor.execute(commandLine, environment, handler);
            handler.waitFor();
        } catch (IOException e) {
            log.warn("OpenCLI spawn failed commandLine={}, message={}", commandLine, e.getMessage());
            throw new OpenCliExecutableFailureException(
                "OpenCLI could not be started (check PATH or executable path): " + commandLine, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenCliException("Interrupted while awaiting OpenCLI subprocess", e, null);
        }

        String stdoutStr = new String(out.toByteArray(), StandardCharsets.UTF_8);
        String stderrStr = new String(err.toByteArray(), StandardCharsets.UTF_8);
        OpenCliParsedFields parsed = OpenCliOutputParser.parseBestEffort(stdoutStr, stderrStr);

        if (watchdog.killedProcess()) {
            OpenCliResult partial = snapshot(stdoutStr, stderrStr, readExitQuietly(handler), parsed);
            throw new OpenCliTimeoutException(
                "OpenCLI timed out after " + timeoutMs + " ms: " + commandLine, partial);
        }

        Exception asyncFailure = handler.getException();
        if (asyncFailure instanceof ExecuteException) {
            ExecuteException ex = (ExecuteException) asyncFailure;
            OpenCliResult failed = snapshot(stdoutStr, stderrStr, normalizeExitValue(ex.getExitValue()), parsed);
            throw new OpenCliNonZeroExitException(
                "OpenCLI failed (exitCode=" + ex.getExitValue() + "): " + commandLine, failed);
        }
        if (asyncFailure != null) {
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
        if (properties.getEnvironment() != null) {
            for (Map.Entry<String, String> e : properties.getEnvironment().entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
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
            .stdout(stdoutStr == null ? "" : stdoutStr)
            .stderr(stderrStr == null ? "" : stderrStr)
            .exitCode(exitCode)
            .success(false)
            .parsed(parsed)
            .build();
    }
}
