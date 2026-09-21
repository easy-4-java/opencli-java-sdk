package io.github.easy4j.opencli.core;

import lombok.Builder;
import lombok.Getter;

/** Immutable execution evidence. Observed byte counts describe bytes read, not bytes produced remotely. */
@Getter
@Builder
public final class OpenCliExecutionDetails {
    /** First terminal condition selected by the invocation owner. */
    public enum TerminationReason {
        PROCESS_EXIT, QUEUE_TIMEOUT, EXECUTION_TIMEOUT, OUTPUT_LIMIT,
        CANCELLED, SPAWN_FAILED, IO_FAILURE, CLEANUP_UNCONFIRMED, RUNTIME_UNAVAILABLE
    }

    /** Confirmation concerns the directly owned child, not an arbitrary process tree. */
    public enum CleanupState { NOT_STARTED, ROOT_EXIT_CONFIRMED, UNCONFIRMED }

    private final TerminationReason terminationReason;
    private final CleanupState cleanupState;
    private final boolean processStarted;
    private final boolean streamsDrained;
    private final long stdoutCapturedBytes;
    private final long stdoutObservedBytes;
    private final boolean stdoutTruncated;
    private final long stderrCapturedBytes;
    private final long stderrObservedBytes;
    private final boolean stderrTruncated;
    private final long elapsedMillis;
    private final long queueWaitMillis;
    /** This portable backend does not claim ownership/termination of detached daemon descendants. */
    private final boolean descendantsExitConfirmed;
}
