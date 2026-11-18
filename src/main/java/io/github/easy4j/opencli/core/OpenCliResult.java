package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.parser.OpenCliParsedFields;
import lombok.Builder;
import lombok.Getter;

/**
 * Raw result for one OpenCLI invocation. Raw output is business data, not a safe
 * diagnostic string. Local execution adds bounded lifecycle evidence; legacy
 * remote responses do not acquire an invented process exit or cleanup state.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Getter
@Builder
public class OpenCliResult {
    private final String stdout;
    private final String stderr;
    private final Integer exitCode;
    private final boolean success;
    private final OpenCliParsedFields parsed;
    /** Only populated by explicit remote HTTP raw capture. */
    private final String remoteRawHttpBody;
    /** Observed local execution metadata; null for a legacy remote result. */
    private final OpenCliExecutionDetails executionDetails;
}
