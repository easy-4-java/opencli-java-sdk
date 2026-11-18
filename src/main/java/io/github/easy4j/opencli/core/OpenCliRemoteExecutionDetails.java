package io.github.easy4j.opencli.core;

import java.util.Objects;
import java.util.OptionalInt;
import lombok.Builder;
import lombok.Getter;

/** Observable evidence from a legacy HTTP collect attempt. */
@Getter
@Builder
public final class OpenCliRemoteExecutionDetails {

    /** The layer that supplied the failed or successful result. */
    public enum Outcome { SUCCESS, COMMAND_FAILURE, HTTP_FAILURE, PROTOCOL_FAILURE, TRANSPORT_FAILURE }

    /** Origin of the compatibility exit code on the associated result. */
    public enum ExitCodeSource { SYNTHETIC_COMPATIBILITY, UNAVAILABLE }

    /** Origin of the items projected to stdout. */
    public enum ItemsSource { RESPONSE, ABSENT_DEFAULT, NULL_DEFAULT, UNAVAILABLE }

    /** HTTP status observed in the response headers, or zero if unavailable. */
    private final int httpStatus;
    private final Outcome outcome;
    /** Null when a business status was unavailable or its envelope failed validation. */
    private final Boolean legacySuccess;
    private final ItemsSource itemsSource;

    /** @return the observed HTTP status, or empty when no response headers arrived */
    public OptionalInt getObservedHttpStatus() {
        return httpStatus > 0 ? OptionalInt.of(httpStatus) : OptionalInt.empty();
    }

    /** @return true if the HTTP exchange failed before a complete response was available */
    public boolean isRemoteOutcomeUnknown() {
        return outcome == Outcome.TRANSPORT_FAILURE;
    }

    /** @return whether the associated 0/1 exit code is a mapping of the business status */
    public ExitCodeSource getExitCodeSource() {
        return Objects.isNull(legacySuccess) ? ExitCodeSource.UNAVAILABLE : ExitCodeSource.SYNTHETIC_COMPATIBILITY;
    }

    /** @return empty because the legacy collect response does not report a process exit */
    public OptionalInt getObservedRemoteExitCode() {
        return OptionalInt.empty();
    }
}
