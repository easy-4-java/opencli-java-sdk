package io.github.hiwepy.opencli.exception;

import io.github.hiwepy.opencli.core.OpenCliResult;

/**
 * Watchdog 终止子进程后判定为超时。
 */
public class OpenCliTimeoutException extends OpenCliException {

    public OpenCliTimeoutException(String message, OpenCliResult partialResult) {
        super(message, partialResult);
    }
}
