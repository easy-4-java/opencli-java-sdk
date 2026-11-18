package io.github.easy4j.opencli.browser.result;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Reported browser analysis metadata. Advice and paths are returned as data. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserAnalysisResult {
    private final String requestedUrl;
    private final String finalUrl;
    private final String title;
    private final String pattern;
    private final JsonNode metadata;

    /** @return a copy of the complete report, including optional sitemap metadata */
    public JsonNode getMetadata() {
        return metadata.deepCopy();
    }
}
