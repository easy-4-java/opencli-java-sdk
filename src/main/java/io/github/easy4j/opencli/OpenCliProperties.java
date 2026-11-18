package io.github.easy4j.opencli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.ToString;

/**
 * OpenCLI runtime configuration without Spring dependencies.
 * Local execution uses a monotonic submission-to-exit deadline; cleanup has a
 * separate finite grace. Remote HTTP uses one invocation budget for transport I/O.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Data
public class OpenCliProperties {
    private OpenCliExecutionTarget executionTarget = OpenCliExecutionTarget.LOCAL_PROCESS;
    /** Root URL of an opencli-admin compatible Agent. */
    @ToString.Exclude
    private String remoteAgentBaseUrl;
    /** Explicit credential header for the configured HTTPS Agent. */
    @ToString.Exclude
    private OpenCliRemoteAuthentication remoteAuthentication;
    private String remoteCollectMode = "cdp";
    private String remoteOutputFormat = "json";
    private String remoteCdpEndpoint = "";
    /** Raw HTTP capture is explicitly opt-in and may contain sensitive business data. */
    private boolean remoteCaptureRawHttpResponse = false;
    /** Maximum decoded HTTP response body bytes accepted from a remote Agent. */
    private int maxRemoteResponseBytes = 10 * 1024 * 1024;
    private String executable = "opencli";
    private String workingDirectory;
    /** Total local queue and execution budget, or the remote HTTP transport budget. */
    private long commandTimeoutMillis = 300_000L;
    /** Legacy per-stream ceiling; nonpositive values keep the finite stdout/stderr budgets. */
    private long maxOutputBytes = 10L * 1024 * 1024;
    /** Positive per-client capacity, zero for max(2, cores); negative is invalid. Captured at construction. */
    private int maxConcurrentExecutions = 0;
    /** Maximum bytes retained from stdout for one local invocation. */
    private int maxStdoutBytes = 8 * 1024 * 1024;
    /** Maximum bytes retained from stderr for one local invocation. */
    private int maxStderrBytes = 2 * 1024 * 1024;
    /** Independent bounded cleanup grace, in milliseconds. */
    private long cleanupGraceMillis = 5_000L;
    private long startupProbeTimeoutMillis = 30_000L;
    /** Literal prefix arguments placed before the command identifier. */
    private List<String> leadingArguments = new ArrayList<>();
    /** Variables overlaying the inherited process environment. */
    private Map<String, String> environment = new LinkedHashMap<>();

    /**
     * Copy into a local-only configuration for reverse-Agent execution without
     * forwarding a received command back to a remote Agent.
     *
     * @return an independent local configuration
     */
    public OpenCliProperties copyForLocalCliExecution() {
        OpenCliProperties c = new OpenCliProperties();
        c.setExecutable(this.executable);
        c.setWorkingDirectory(this.workingDirectory);
        c.setCommandTimeoutMillis(this.commandTimeoutMillis);
        c.setMaxOutputBytes(this.maxOutputBytes);
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
}
