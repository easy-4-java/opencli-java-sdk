package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.availability.OpenCliAvailabilityReport;
import lombok.Getter;

/**
 * 应用启动阶段 OpenCLI 不可用且配置为 fail-fast 时抛出。
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 1.0.0
 */
@Getter
public class OpenCliStartupException extends OpenCliException {

    private final OpenCliAvailabilityReport availabilityReport;

    /**
     * @param message 诊断说明
     * @param report    探测报告
     */
    public OpenCliStartupException(String message, OpenCliAvailabilityReport report) {
        super(message, report != null ? report.getProbeResult() : null);
        this.availabilityReport = report;
    }
}
