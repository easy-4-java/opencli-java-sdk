package io.github.easy4j.opencli.browser.result;

import tools.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Navigation metadata from the pinned OpenCLI browser open contract. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserOpenResult {
    private final String url;
    /** Present when OpenCLI reports the active page identifier. */
    private final String pageId;
    private final JsonNode metadata;

    /** @return all reported fields, including optional sitemap or trace data */
    public JsonNode getMetadata() {
        return metadata.deepCopy();
    }
}
