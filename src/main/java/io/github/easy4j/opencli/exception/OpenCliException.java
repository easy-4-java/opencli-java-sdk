package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;
import lombok.Getter;

/**
 * OpenCLI 子进程执行层的基础运行时异常。
 * <p>
 * 可选携带最近一次可观测的 {@link OpenCliResult}，便于调用方在重试、降级或记录时保留 stdout/stderr。
 * </p>
 */
@Getter
public class OpenCliException extends RuntimeException {

    private final OpenCliResult partialResult;

    /**
     * @param message        技术性说明
     * @param cause          原始原因，可为 null
     * @param partialResult 失败前快照，可为 null
     */
    public OpenCliException(String message, Throwable cause, OpenCliResult partialResult) {
        super(message, cause);
        this.partialResult = partialResult;
    }

    /**
     * @param message        技术性说明
     * @param partialResult 失败前快照，可为 null
     */
    public OpenCliException(String message, OpenCliResult partialResult) {
        super(message);
        this.partialResult = partialResult;
    }
}
