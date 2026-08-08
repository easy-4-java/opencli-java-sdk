package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/**
 * Thrown when the watchdog terminates the subprocess,判定 as a timeout.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */public class OpenCliTimeoutException extends OpenCliException {

    public OpenCliTimeoutException(String message, OpenCliResult partialResult) {
        super(message, partialResult);
    }
}
