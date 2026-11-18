package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.parser.OpenCliHelpResult;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/** Immutable observation of the configured local CLI's root help. */
@Getter
@Builder
public final class OpenCliCapabilitySnapshot {
    public enum Status { AVAILABLE, UNKNOWN, UNSUPPORTED }

    private final Status status;
    private final String reason;
    private final String executable;
    private final String observedVersion;
    private final Instant observedAt;
    private final String configurationSha256;
    private final String helpSha256;
    private final OpenCliHelpResult help;

    /** @return scope of the metadata probe */
    public String getScope() {
        return "ROOT_HELP";
    }

    /** @return unresolved origin, since help does not attribute overrides or plugins */
    public String getCommandOrigin() {
        return "UNKNOWN";
    }
}
