package io.github.easy4j.opencli.browser.result;

import tools.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Tab metadata from the pinned OpenCLI browser tab list contract. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserTab {
    private final int index;
    private final String pageId;
    private final boolean active;
    private final String url;
    private final String title;
    private final JsonNode metadata;

    /** @return an independent JSON view containing all fields and the original identifier type */
    public JsonNode getMetadata() {
        return metadata.deepCopy();
    }
}
