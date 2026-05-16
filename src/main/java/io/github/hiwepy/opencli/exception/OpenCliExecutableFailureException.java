package io.github.hiwepy.opencli.exception;

import io.github.hiwepy.opencli.core.OpenCliResult;

/**
 * 无法启动 OpenCLI 可执行文件（PATH/权限/参数非法等）时抛出。
 */
public class OpenCliExecutableFailureException extends OpenCliException {

    public OpenCliExecutableFailureException(String message, Throwable cause) {
        super(message, cause, null);
    }
}
