package io.github.easy4j.opencli.parser;

import lombok.Getter;

/** Result of interpreting one supplied OpenCLI help document. */
@Getter
public final class OpenCliHelpResult {

    public enum Status { AVAILABLE, UNKNOWN }

    public enum Reason { EMPTY, UNSUPPORTED_FORMAT, INVALID_JSON, INVALID_SCHEMA }

    private final Status status;
    /** Null for recognized help. */
    private final Reason reason;
    /** Exact text passed to the parser, including any trailing output. */
    private final String rawHelp;
    /** Available only for recognized structured help. */
    private final OpenCliHelpSchema schema;

    OpenCliHelpResult(Status status, Reason reason, String rawHelp, OpenCliHelpSchema schema) {
        this.status = status;
        this.reason = reason;
        this.rawHelp = rawHelp;
        this.schema = schema;
    }
}
