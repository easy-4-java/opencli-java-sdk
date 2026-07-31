package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/**
 * Watchdog 终止子进程后判定为超时。
 */
public class OpenCliTimeoutException extends OpenCliException {

    public OpenCliTimeoutException(String message, OpenCliResult partialResult) {
        super(message, partialResult);
    }
}
