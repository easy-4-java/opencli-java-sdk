package io.github.easy4j.opencli.exception;

/** 远端 /collect 响应不符合协议，保留既有远程异常捕获类型。 */
public final class OpenCliRemoteProtocolException extends OpenCliExecutableFailureException {

    public OpenCliRemoteProtocolException(String message, String correlationId) {
        super(message + " correlationId=" + correlationId, null);
    }
}
