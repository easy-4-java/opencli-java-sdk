package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/**
 * 无法启动 OpenCLI 可执行文件（PATH/权限/参数非法等）时抛出。
 */
public class OpenCliExecutableFailureException extends OpenCliException {

    public OpenCliExecutableFailureException(String message, Throwable cause) {
        super(message, cause, null);
    }
}
