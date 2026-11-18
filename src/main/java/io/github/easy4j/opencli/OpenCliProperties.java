package io.github.easy4j.opencli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.ToString;

/**
 * OpenCLI runtime configuration POJO with no Spring dependency.
 * <p>
 * 描述可执行文件、工作目录、超时、全局 argv 前缀、远端 Agent 以及需要注入子进程的环境变量
 *（例如 {@code OPENCLI_CDP_ENDPOINT}）。Spring Boot 可由上层以
 * {@code @ConfigurationProperties(prefix = "opencli")} 绑定同名字段。
 * </p>
 * <p>
 * {@link #commandTimeoutMillis} 在本地模式下用于子进程 Watchdog；在
 * {@link OpenCliExecutionTarget#REMOTE_AGENT_HTTP} 模式下为连接池等待、建连和读取响应共享的总预算。
 * </p>
 */
@Data/**

 * OpenCLI runtime configuration POJO with no Spring dependency.
 *
 * <p>Describes the executable path, working directory, timeout, global argv prefix,
 * remote Agent settings, and environment variables injected into the subprocess
 * (e.g.&nbsp;{@code OPENCLI_CDP_ENDPOINT}). Spring Boot applications can bind these
 * fields via {@code @ConfigurationProperties(prefix = "opencli")}.</p>
 *
 * <p>{@link #commandTimeoutMillis} is used as the subprocess watchdog timeout in local
 * mode and as the HTTP client timeout in {@link OpenCliExecutionTarget#REMOTE_AGENT_HTTP} mode.</p>

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public class OpenCliProperties {

    /**
     * 执行目标：本机进程或与 opencli-admin 兼容的远端 Agent。
     */
    private OpenCliExecutionTarget executionTarget = OpenCliExecutionTarget.LOCAL_PROCESS;

    /**
     * 远端 Agent 根 URL（不含尾斜杠），例如 {@code http://192.168.1.10:19823}。
     * <p>仅当 {@link #executionTarget} 为 {@link OpenCliExecutionTarget#REMOTE_AGENT_HTTP} 时必填。</p>
     */
    @ToString.Exclude
    private String remoteAgentBaseUrl;

    /** Explicit credential header for the configured HTTPS Agent. */
    @ToString.Exclude
    private OpenCliRemoteAuthentication remoteAuthentication;

    /**
     * 传给 Agent collect 的 {@code mode}：{@code bridge} 或 {@code cdp}（与 Agent 环境一致）。
     */
    private String remoteCollectMode = "cdp";

    /**
     * 传给 Agent collect 的默认 {@code format}（可被 argv 中的 {@code -f} 覆盖）。
     */
    private String remoteOutputFormat = "json";

    /**
     * 对应 collect body 的 {@code cdp_endpoint}；空表示由 Agent 使用自身 {@code OPENCLI_CDP_ENDPOINT}。
     */
    private String remoteCdpEndpoint = "";

    /**
     * 为 true 时，远程模式下将 Agent HTTP 响应原文写入 {@link io.github.easy4j.opencli.core.OpenCliResult} 的
     * {@code remoteRawHttpBody} 字段；
     * 本地模式无效果。大响应时请谨慎开启。
     */
    private boolean remoteCaptureRawHttpResponse = false;
    /** Maximum decoded HTTP response body bytes accepted from a remote Agent. */
    private int maxRemoteResponseBytes = 10 * 1024 * 1024;

    /**
     * OpenCLI 可执行文件名或绝对路径；默认假定已在 {@code PATH} 中。
     */
    private String executable = "opencli";
    /** Defaults applied when dispatching a schema-validated local command. */
    private OpenCliInvocationContext invocationDefaults;

    /**
     * 子进程工作目录；为空时使用 JVM 当前目录。
     */
    private String workingDirectory;

    /**
     * 单次调用超时（毫秒）：本地模式用于子进程 Watchdog；远程模式下覆盖 Agent HTTP 传输全过程。
     */
    private long commandTimeoutMillis = 300_000L;

    /**
     * 子进程单流（stdout/stderr 各自计）输出捕获上限（字节）。超过部分丢弃，
     * 并在结果字符串尾部追加截断标记；{@code <= 0} 表示不限制。默认 10 MiB。
     */
    private long maxOutputBytes = 10L * 1024 * 1024;

    /**
     * 本机 CLI 子进程最大并发数；小于等于 0 时使用 CPU 核心数与 2 的较大值。
     */
    private int maxConcurrentExecutions = 0;

    /**
     * 启动探测（{@code opencli list}）专用超时（毫秒）；小于等于 0 时探测使用 30 秒。
     */
    private long startupProbeTimeoutMillis = 30_000L;

    /**
     * 附加到 {@code opencli} 之后的<strong>最前</strong>参数（在 adapter 名之前），便于预留 profile 等扩展。
     */
    private List<String> leadingArguments = new ArrayList<>();

    /**
     * 合并进子进程环境的键值；覆盖同名系统环境变量。
     */
    private Map<String, String> environment = new LinkedHashMap<>();

    /**
     * 复制为「仅本机子进程」配置，供边缘 WebSocket Agent 处理中心下发的 {@code collect} 时使用，
     * 避免误将 collect 再次转发为 {@link OpenCliExecutionTarget#REMOTE_AGENT_HTTP} 而形成回路。
     *
     * @return 新实例，不会改变当前对象
     */
    public OpenCliProperties copyForLocalCliExecution() {
        OpenCliProperties c = new OpenCliProperties();
        c.setExecutable(this.executable);
        c.setInvocationDefaults(this.invocationDefaults);
        c.setWorkingDirectory(this.workingDirectory);
        c.setCommandTimeoutMillis(this.commandTimeoutMillis);
        c.setMaxConcurrentExecutions(this.maxConcurrentExecutions);
        c.setMaxStdoutBytes(this.maxStdoutBytes);
        c.setMaxStderrBytes(this.maxStderrBytes);
        c.setCleanupGraceMillis(this.cleanupGraceMillis);
        c.setStartupProbeTimeoutMillis(this.startupProbeTimeoutMillis);
        c.setLeadingArguments(new ArrayList<>(this.leadingArguments));
        c.setEnvironment(new LinkedHashMap<>(this.environment));
        c.setExecutionTarget(OpenCliExecutionTarget.LOCAL_PROCESS);
        return c;
    }

    /** Maximum stdout bytes captured per invocation. */
    private int maxStdoutBytes = 8 * 1024 * 1024;
    /** Maximum stderr bytes captured per invocation. */
    private int maxStderrBytes = 2 * 1024 * 1024;
    /** Maximum time to wait for owned resources after execution ends. */
    private long cleanupGraceMillis = 5_000L;
}
