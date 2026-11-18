package io.github.easy4j.opencli.browser.result;

import tools.jackson.databind.JsonNode;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** One bounded browser console capture. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserConsoleResult {
    private final String session;
    private final String capturedAt;
    private final List<Message> messages;
    private final JsonNode metadata;

    /** @return a copy of the capture envelope */
    public JsonNode getMetadata() {
        return metadata.deepCopy();
    }

    /** A console message with its original browser-supplied fields. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Message {
        private final String type;
        private final String text;
        private final String timestamp;
        private final JsonNode metadata;

        /** @return a copy of the original console record */
        public JsonNode getMetadata() {
            return metadata.deepCopy();
        }
    }
}
