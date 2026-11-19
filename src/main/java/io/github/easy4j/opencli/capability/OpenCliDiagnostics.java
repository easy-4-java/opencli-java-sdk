package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.OpenCliExecutionTarget;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Read-only diagnostic observations. Each check carries its own provenance. */
@Getter
public final class OpenCliDiagnostics {
    public enum Check { EXECUTABLE, VERSION, STRUCTURED_HELP, BROWSER_BRIDGE, AUTHENTICATION, DAEMON, REMOTE_TRANSPORT }
    public enum Status { AVAILABLE, UNKNOWN, STALE, NOT_APPLICABLE }
    public enum Source { CONFIGURATION, FILESYSTEM, VERSION_PROBE, PREVIOUS_DISCOVERY, UNVERIFIED }

    private final OpenCliExecutionTarget target;
    private final String version;
    private final Map<Check, Observation> observations;

    private OpenCliDiagnostics(OpenCliExecutionTarget target, String version, Map<Check, Observation> observations) {
        this.target = target;
        this.version = version;
        this.observations = Collections.unmodifiableMap(new EnumMap<>(observations));
    }

    /** @param check diagnostic check @return its independent observation */
    public Observation observation(Check check) {
        return observations.get(Objects.requireNonNull(check, "check"));
    }

    static OpenCliDiagnostics remote() {
        Instant observedAt = Instant.now();
        Map<Check, Observation> checks = new EnumMap<>(Check.class);
        for (Check check : Check.values()) {
            checks.put(check, check == Check.REMOTE_TRANSPORT
                ? new Observation(Status.UNKNOWN, Source.CONFIGURATION, observedAt, "REMOTE_TRANSPORT_NOT_PROBED")
                : new Observation(Status.NOT_APPLICABLE, Source.CONFIGURATION, observedAt, "LOCAL_CHECK_IN_REMOTE_MODE"));
        }
        return new OpenCliDiagnostics(OpenCliExecutionTarget.REMOTE_AGENT_HTTP, null, checks);
    }

    static OpenCliDiagnostics local(OpenCliCapabilitySnapshot probe, OpenCliCapabilitySnapshot historicalHelp) {
        Instant observedAt = probe.getObservedAt();
        Map<Check, Observation> checks = new EnumMap<>(Check.class);
        String version = probe.getObservedVersion();
        String reason = probe.getReason();
        checks.put(Check.EXECUTABLE, Objects.nonNull(probe.getExecutable())
            ? new Observation(Status.AVAILABLE, Source.FILESYSTEM, observedAt, "EXECUTABLE_PATH_VERIFIED")
            : new Observation(Status.UNKNOWN, Source.CONFIGURATION, observedAt, reason));
        checks.put(Check.VERSION, Objects.nonNull(version)
            ? new Observation(Status.AVAILABLE, Source.VERSION_PROBE, observedAt, null)
            : new Observation(Status.UNKNOWN, Objects.isNull(probe.getExecutable())
                ? Source.CONFIGURATION : Source.VERSION_PROBE, observedAt, reason));
        checks.put(Check.STRUCTURED_HELP, helpObservation(probe, historicalHelp));
        Observation unverified = new Observation(Status.UNKNOWN, Source.UNVERIFIED, observedAt,
            "READ_ONLY_PROBE_UNVERIFIED");
        checks.put(Check.BROWSER_BRIDGE, unverified);
        checks.put(Check.AUTHENTICATION, unverified);
        checks.put(Check.DAEMON, unverified);
        checks.put(Check.REMOTE_TRANSPORT, new Observation(Status.NOT_APPLICABLE, Source.CONFIGURATION,
            observedAt, "REMOTE_CHECK_IN_LOCAL_MODE"));
        return new OpenCliDiagnostics(OpenCliExecutionTarget.LOCAL_PROCESS, version, checks);
    }

    private static Observation helpObservation(OpenCliCapabilitySnapshot probe, OpenCliCapabilitySnapshot previous) {
        if (Objects.isNull(previous) || Objects.isNull(probe.getObservedVersion())
            || !probe.getObservedVersion().equals(previous.getObservedVersion())
            || !Objects.equals(probe.getConfigurationSha256(), previous.getConfigurationSha256())) {
            return new Observation(Status.UNKNOWN, Source.UNVERIFIED, probe.getObservedAt(), "HELP_NOT_OBSERVED");
        }
        boolean available = previous.getStatus() == OpenCliCapabilitySnapshot.Status.AVAILABLE
            || previous.getStatus() == OpenCliCapabilitySnapshot.Status.STALE;
        String reason = Objects.isNull(previous.getReason()) ? "HELP_NOT_REVALIDATED" : previous.getReason();
        return new Observation(available ? Status.STALE : Status.UNKNOWN, Source.PREVIOUS_DISCOVERY,
            previous.getObservedAt(), reason);
    }

    /** Evidence and applicability for a single check. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Observation {
        private final Status status;
        private final Source source;
        private final Instant observedAt;
        private final String reason;
    }
}
