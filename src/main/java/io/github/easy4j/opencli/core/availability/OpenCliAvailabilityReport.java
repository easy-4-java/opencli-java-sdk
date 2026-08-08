package io.github.easy4j.opencli.core.availability;

import io.github.easy4j.opencli.core.OpenCliResult;
import lombok.Builder;
import lombok.Getter;

/**
 * OpenCLI 启动/就绪探测结果。
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 1.0.0
 */
@Getter
@Builder/**

 * Result of an OpenCLI startup/readiness probe.

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public class OpenCliAvailabilityReport {

    private final OpenCliAvailabilityStatus status;
    private final boolean available;
    private final String configuredExecutable;
    private final String resolvedExecutablePath;
    private final String message;
    private final OpenCliResult probeResult;

    /**
     * @return 是否可继续启动（含远程模式跳过）
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 构造面向日志/异常的诊断文本。
     *
     * @return 说明字符串
     */
    public String toDiagnosticMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("OpenCLI ");
        sb.append(available ? "ready" : "unavailable");
        sb.append(" [").append(status).append(']');
        if (configuredExecutable != null) {
            sb.append(" executable=").append(configuredExecutable);
        }
        if (resolvedExecutablePath != null) {
            sb.append(" resolved=").append(resolvedExecutablePath);
        }
        if (message != null && !message.isEmpty()) {
            sb.append(" — ").append(message);
        }
        return sb.toString();
    }
}
