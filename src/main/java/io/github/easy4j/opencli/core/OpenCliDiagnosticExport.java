package io.github.easy4j.opencli.core;

import java.util.Objects;

/**
 * Creates an explicit, in-memory diagnostic projection of an existing invocation result.
 * The SDK retains no copy of the returned text. The caller controls its retention and destination.
 */
public final class OpenCliDiagnosticExport {

    private static final String COMPLETE_HEADER = "policy=metadata-only\ntruncated=false\n";
    private static final String TRUNCATED_HEADER = "policy=metadata-only\ntruncated=true\n";

    /** Minimum budget that can represent either export header. */
    public static final int MIN_EXPORT_BYTES = COMPLETE_HEADER.length();

    private OpenCliDiagnosticExport() {
    }

    /**
     * Exports status and character counts as UTF-8 key-value lines. Raw output, parsed values,
     * command arguments, URLs, and paths are excluded by construction. Lengths are UTF-16 code-unit counts
     * and reveal the size of the corresponding fields.
     *
     * @param result completed result or partial result from an exception
     * @param maxBytes maximum UTF-8 size, at least {@link #MIN_EXPORT_BYTES}
     * @return diagnostic text containing complete fields only, with a truncation indicator
     * @throws IllegalArgumentException when the budget cannot fit the header
     */
    public static String export(OpenCliResult result, int maxBytes) {
        Objects.requireNonNull(result, "result");
        if (maxBytes < MIN_EXPORT_BYTES) {
            throw new IllegalArgumentException("maxBytes must fit the diagnostic export header");
        }

        Integer exitCode = result.getExitCode();
        String[] fields = {
            "success=" + result.isSuccess() + "\n",
            Objects.isNull(exitCode) ? "" : "exitCode=" + exitCode + "\n",
            characterCount("stdoutChars", result.getStdout()),
            characterCount("stderrChars", result.getStderr()),
            characterCount("rawHttpChars", result.getRemoteRawHttpBody())
        };

        // Every emitted character is ASCII, so the character count is also the UTF-8 byte count.
        int completeBytes = COMPLETE_HEADER.length();
        for (String field : fields) {
            completeBytes += field.length();
        }

        boolean truncated = completeBytes > maxBytes;
        StringBuilder output = new StringBuilder(Math.min(completeBytes, maxBytes));
        output.append(truncated ? TRUNCATED_HEADER : COMPLETE_HEADER);
        int remainingBytes = maxBytes - COMPLETE_HEADER.length();
        for (String field : fields) {
            if (field.length() > remainingBytes) {
                break;
            }
            output.append(field);
            remainingBytes -= field.length();
        }
        return output.toString();
    }

    private static String characterCount(String name, String value) {
        return Objects.isNull(value) ? "" : name + "=" + value.length() + "\n";
    }
}
