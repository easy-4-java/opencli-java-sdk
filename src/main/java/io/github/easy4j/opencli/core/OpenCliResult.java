package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import lombok.Builder;
import lombok.Getter;

/**
 * 单次 OpenCLI 调用的原始结果载体。
 * <p>
 * {@link #remoteRawHttpBody} 仅在 {@link io.github.hiwepy.opencli.OpenCliExecutionTarget#REMOTE_AGENT_HTTP}
 * 且 {@link io.github.hiwepy.opencli.OpenCliProperties#isRemoteCaptureRawHttpResponse()} 为 true 时填充，
 * 为 Agent 返回的完整 HTTP 响应体，便于审计或与 {@link #getStdout()}（由 {@code items} 重组）对照。
 * </p>
 */
@Getter
@Builder
public class OpenCliResult {

    private final String stdout;

    private final String stderr;

    private final Integer exitCode;

    private final boolean success;

    private final OpenCliParsedFields parsed;

    /**
     * 远端 Agent HTTP 响应全文；本地子进程模式或非调试场景下为 null。
     */
    private final String remoteRawHttpBody;
}
