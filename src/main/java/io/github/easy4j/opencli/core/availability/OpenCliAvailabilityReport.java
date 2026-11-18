package io.github.easy4j.opencli.core.availability;

import io.github.easy4j.opencli.core.OpenCliResult;
import lombok.Builder;
import lombok.Getter;

/**
 * OpenCLI 启动/就绪探测结果。
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Getter
@Builder/**

 * Result of an OpenCLI startup/readiness probe.

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

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
     * 构造可写入日志的就绪状态摘要。可执行文件和原始探测消息可通过各自的 getter 读取。
     *
     * @return 说明字符串
     */
    public String toDiagnosticMessage() {
        return "OpenCLI " + (available ? "ready" : "unavailable") + " [" + status + "]";
    }
}
