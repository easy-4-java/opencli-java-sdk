package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/** Failure to start the configured executable. */
public class OpenCliExecutableFailureException extends OpenCliException {
    public OpenCliExecutableFailureException(String message, Throwable cause) {
        super(message, cause, null);
    }

    /** Retains a no-process-started snapshot without inventing an exit code. */
    public OpenCliExecutableFailureException(String message, Throwable cause, OpenCliResult partialResult) {
        super(message, cause, partialResult);
    }
}
