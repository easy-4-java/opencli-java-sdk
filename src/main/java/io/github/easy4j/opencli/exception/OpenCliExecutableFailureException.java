package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/**
 * Thrown when the OpenCLI executable cannot be started (PATH, permissions, invalid arguments, etc.).
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */public class OpenCliExecutableFailureException extends OpenCliException {

    public OpenCliExecutableFailureException(String message, Throwable cause) {
        super(message, cause, null);
    }
}
