package io.github.easy4j.opencli.parser;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/** Immutable interpretation of a local failure's bounded stderr. Raw stderr remains on the execution result. */
@Getter
@Builder(access = AccessLevel.PACKAGE)
public final class OpenCliErrorEnvelope {
    public enum Status { DECODED, EMPTY, NO_ENVELOPE, INCOMPLETE, OVER_BUDGET, INVALID }
    public enum Format { JSON, YAML }

    private final Status status;
    private final Format format;
    private final Integer observedProcessExit;
    private final Integer reportedExit;
    private final String code;
    private final String message;
    private final String help;

    /** Original error object, including cause, stack and unknown fields. */
    @Builder.Default
    private final Map<String, Object> errorFields = Collections.emptyMap();

    /** Original envelope, including trace and unknown fields. */
    @Builder.Default
    private final Map<String, Object> envelopeFields = Collections.emptyMap();

    /** Trace metadata. Paths are returned as data. */
    @Builder.Default
    private final Map<String, Object> trace = Collections.emptyMap();

    /** @return whether both exit values are available and disagree */
    public boolean isExitMismatch() {
        return Objects.nonNull(observedProcessExit) && Objects.nonNull(reportedExit)
            && !observedProcessExit.equals(reportedExit);
    }
}
