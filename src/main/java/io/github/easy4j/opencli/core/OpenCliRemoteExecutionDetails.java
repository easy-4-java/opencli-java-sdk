package io.github.easy4j.opencli.core;

import java.util.Objects;
import java.util.OptionalInt;
import lombok.Builder;
import lombok.Getter;

/** Observable evidence from an HTTP response to a legacy collect request. */
@Getter
@Builder
public final class OpenCliRemoteExecutionDetails {

    /** The layer that supplied the failed or successful result. */
    public enum Outcome { SUCCESS, COMMAND_FAILURE, HTTP_FAILURE, PROTOCOL_FAILURE }

    /** Origin of the compatibility exit code on the associated result. */
    public enum ExitCodeSource { SYNTHETIC_COMPATIBILITY, UNAVAILABLE }

    /** Origin of the items projected to stdout. */
    public enum ItemsSource { RESPONSE, ABSENT_DEFAULT, NULL_DEFAULT, UNAVAILABLE }

    private final int httpStatus;
    private final Outcome outcome;
    /** Null when a business status was unavailable or its envelope failed validation. */
    private final Boolean legacySuccess;
    private final ItemsSource itemsSource;

    /** @return whether the associated 0/1 exit code is a mapping of the business status */
    public ExitCodeSource getExitCodeSource() {
        return Objects.isNull(legacySuccess) ? ExitCodeSource.UNAVAILABLE : ExitCodeSource.SYNTHETIC_COMPATIBILITY;
    }

    /** @return empty because the legacy collect response does not report a process exit */
    public OptionalInt getObservedRemoteExitCode() {
        return OptionalInt.empty();
    }
}
