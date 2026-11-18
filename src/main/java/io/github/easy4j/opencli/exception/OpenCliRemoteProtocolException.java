package io.github.easy4j.opencli.exception;

import io.github.easy4j.opencli.core.OpenCliResult;

/** 远端 /collect 响应不符合协议，保留既有远程异常捕获类型。 */
public final class OpenCliRemoteProtocolException extends OpenCliExecutableFailureException {

    public OpenCliRemoteProtocolException(String message, String correlationId) {
        this(message, correlationId, null);
    }

    /**
     * @param message safe summary of the protocol failure
     * @param correlationId SDK request correlation identifier
     * @param partialResult bounded HTTP response evidence
     */
    public OpenCliRemoteProtocolException(String message, String correlationId, OpenCliResult partialResult) {
        super(message + " correlationId=" + correlationId, null, partialResult);
    }
}
