package io.github.easy4j.opencli.exception;

import lombok.Getter;

/** 远端 HTTP 响应体超出允许字节数，保留 HTTP 状态及上限。 */
@Getter
public final class OpenCliRemoteResponseLimitException extends OpenCliExecutableFailureException {

    private final int httpStatus;
    private final int maxResponseBytes;

    /**
     * @param httpStatus HTTP 响应状态码
     * @param maxResponseBytes 客户端允许读取的最大响应字节数
     * @param correlationId SDK 请求关联标识
     */
    public OpenCliRemoteResponseLimitException(int httpStatus, int maxResponseBytes, String correlationId) {
        super("Agent HTTP response body exceeded " + maxResponseBytes + " bytes, status="
            + httpStatus + " correlationId=" + correlationId, null);
        this.httpStatus = httpStatus;
        this.maxResponseBytes = maxResponseBytes;
    }
}
