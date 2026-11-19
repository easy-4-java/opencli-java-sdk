package io.github.easy4j.opencli.browser.result;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigInteger;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Browser selector matches, including the total before the entry limit. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserFindResult {
    private final long matchesN;
    private final List<Entry> entries;
    private final JsonNode metadata;

    /** @return a copy of the original result with all reported fields */
    public JsonNode getMetadata() {
        return metadata.deepCopy();
    }

    /** A matched element. The optional numeric reference retains its exact value. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Entry {
        private final long nth;
        private final BigInteger ref;
        private final String tag;
        private final String role;
        private final String text;
        private final boolean visible;
        private final JsonNode metadata;

        /** @return a copy including attributes, compound data and extensions */
        public JsonNode getMetadata() {
            return metadata.deepCopy();
        }
    }
}
