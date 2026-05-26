package io.github.hiwepy.opencli.core.availability;

import io.github.hiwepy.opencli.core.OpenCliResult;
import lombok.Builder;
import lombok.Getter;

/**
 * OpenCLI 启动/就绪探测结果。
 *
 * @author wandl
 * @since 1.0.0
 */
@Getter
@Builder
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
