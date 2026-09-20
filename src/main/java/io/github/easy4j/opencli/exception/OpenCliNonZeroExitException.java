package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/**
 * Thrown when the subprocess exits with a non-zero code or corresponds to an
 * {@link org.apache.commons.exec.ExecuteException} failure.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public class OpenCliNonZeroExitException extends OpenCliException {

    public OpenCliNonZeroExitException(String message, OpenCliResult failed) {
        super(message, failed);
    }
}
