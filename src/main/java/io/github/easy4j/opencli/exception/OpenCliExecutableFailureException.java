package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/**
 * Thrown when the OpenCLI executable cannot be started (PATH, permissions, invalid arguments, etc.).
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public class OpenCliExecutableFailureException extends OpenCliException {

    public OpenCliExecutableFailureException(String message, Throwable cause) {
        super(message, cause, null);
    }
}
