package io.github.easy4j.opencli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * OpenCLI runtime configuration without Spring dependencies.
 * Local execution uses a monotonic submission-to-exit deadline; cleanup has a
 * separate finite grace. Remote HTTP retains its own transport timeout semantics.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Data
public class OpenCliProperties {
    private OpenCliExecutionTarget executionTarget = OpenCliExecutionTarget.LOCAL_PROCESS;
    /** Root URL of an opencli-admin compatible Agent. */
    private String remoteAgentBaseUrl;
    private String remoteCollectMode = "cdp";
    private String remoteOutputFormat = "json";
    private String remoteCdpEndpoint = "";
    /** Raw HTTP capture is explicitly opt-in and may contain sensitive business data. */
    private boolean remoteCaptureRawHttpResponse = false;
    private String executable = "opencli";
    private String workingDirectory;
    /** Total local queue plus execution budget; remote mode uses an HTTP timeout. */
    private long commandTimeoutMillis = 300_000L;
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
