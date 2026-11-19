package io.github.easy4j.opencli.exception;

import lombok.Getter;

/** Legacy HTTP collect exhausted its time budget before receiving a verified response. */
@Getter
public final class OpenCliRemoteTimeoutException extends OpenCliExecutableFailureException {

    private final int timeoutMillis;
    private final boolean remoteOutcomeUnknown;

    /**
     * @param timeoutMillis original HTTP invocation budget
     * @param remoteOutcomeUnknown whether the request was submitted to the transport
     * @param correlationId SDK request correlation identifier
     */
    public OpenCliRemoteTimeoutException(int timeoutMillis, boolean remoteOutcomeUnknown, String correlationId) {
        super("Agent HTTP deadline exceeded after " + timeoutMillis + " ms, "
            + (remoteOutcomeUnknown ? "remote outcome unknown" : "request not dispatched")
            + " correlationId=" + correlationId, null);
        this.timeoutMillis = timeoutMillis;
        this.remoteOutcomeUnknown = remoteOutcomeUnknown;
    }
}
