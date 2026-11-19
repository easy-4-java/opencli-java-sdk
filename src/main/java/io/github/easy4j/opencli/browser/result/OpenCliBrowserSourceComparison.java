package io.github.easy4j.opencli.browser.result;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** DOM and AX observations from the same browser state comparison. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserSourceComparison {
    private final String url;
    private final Metrics dom;
    private final Metrics ax;
    private final JsonNode metadata;

    /** @return an independent copy of the entire comparison object */
    public JsonNode getMetadata() {
        return metadata.deepCopy();
    }

    /** A single source observation. Counts are absent when the source failed. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Metrics {
        private final boolean ok;
        private final long elapsedMillis;
        private final Long chars;
        private final Long bytes;
        private final Long lines;
        private final Long approximateTokens;
        private final Long refs;
        private final Long frameSections;
        private final Long interactive;
        private final String errorCode;
        private final String errorMessage;
    }
}
