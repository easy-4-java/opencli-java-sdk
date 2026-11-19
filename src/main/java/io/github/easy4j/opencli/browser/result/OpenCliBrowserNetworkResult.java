package io.github.easy4j.opencli.browser.result;

import tools.jackson.databind.JsonNode;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** One network capture, with any upstream filtering, cache and truncation metadata. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserNetworkResult {
    private final String session;
    private final String capturedAt;
    private final long filteredOut;
    private final List<Entry> entries;
    private final JsonNode metadata;

    /** @return a copy of the capture envelope */
    public JsonNode getMetadata() {
        return metadata.deepCopy();
    }

    /** An observed request. Metadata includes the preview or body supplied by OpenCLI. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Entry {
        private final String key;
        private final String method;
        private final int status;
        private final String url;
        private final String contentType;
        private final long size;
        private final String timestamp;
        private final JsonNode metadata;

        /** @return a copy including any body, shape, truncation and request metadata */
        public JsonNode getMetadata() {
            return metadata.deepCopy();
        }
    }
}
