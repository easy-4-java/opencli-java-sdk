package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/**
 * 子进程非零退出或与 {@link org.apache.commons.exec.ExecuteException} 对应失败。
 */
public class OpenCliNonZeroExitException extends OpenCliException {

    public OpenCliNonZeroExitException(String message, OpenCliResult failed) {
        super(message, failed);
    }
}
