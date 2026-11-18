package io.github.easy4j.opencli.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class OpenCliStructuredHelpFixtureTest {

    private static final String FIXTURES = "/opencli-contracts/v1/structured-help-1.8.8/";
    private static final ObjectReader STRICT_JSON = new JsonMapper().reader()
        .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    @Test
    void preservesTheRootMetaAndSiteShapes() throws IOException {
        JsonNode root = read("root.json");
        assertEquals("opencli", root.path("name").asText());
        assertTrue(includesNamed(root.path("commands"), "list"));
        assertTrue(root.path("site_adapters").isObject());
        assertTrue(root.path("site_adapters").path("sites").isArray());

        JsonNode auth = read("meta-auth.json");
        assertEquals("opencli auth", auth.path("command").asText());
        assertTrue(auth.path("namespace_options").isArray());
        assertTrue(auth.path("global_options").isArray());

        JsonNode site = read("site-npm.json");
        assertEquals("npm", site.path("site").asText());
        assertTrue(includesNamed(site.path("commands"), "search"));
        assertTrue(site.path("common_options").isArray());

        JsonNode leaf = read("site-npm-search.json");
        assertEquals("opencli npm search", leaf.path("command").asText());
        assertEquals("query", leaf.path("positionals").get(0).path("name").asText());
        assertTrue(leaf.path("positionals").get(0).path("required").booleanValue());
        assertTrue(includesNamed(leaf.path("command_options"), "limit"));
    }

    @Test
    void preservesBrowserSessionPlaceholdersAndOptionScopes() throws IOException {
        JsonNode tabs = read("browser-tab.json");
        assertEquals("opencli browser <session> tab", tabs.path("command").asText());
        assertTrue(includesNamed(tabs.path("commands"), "tab list"));
        assertTrue(includesNamed(tabs.path("namespace_options"), "window"));
        assertTrue(includesNamed(tabs.path("global_options"), "profile"));
        assertTrue(includesNamed(tabs.path("commands").get(0).path("command_options"), "tab"));

        JsonNode get = read("browser-get.json");
        assertEquals("opencli browser <session> get", get.path("command").asText());
        assertTrue(includesNamed(get.path("commands"), "get text"));

        JsonNode state = read("browser-state.json");
        assertEquals("opencli browser <session> state", state.path("command").asText());
    }

    @Test
    void identifiesRealOutputsThatAreNotSingleJsonDocuments() {
        assertThrows(JsonProcessingException.class, () -> read("browser.stdout"));
        assertThrows(JsonProcessingException.class, () -> read("meta-list.txt"));
    }

    private static boolean includesNamed(JsonNode entries, String name) {
        for (JsonNode entry : entries) {
            if (name.equals(entry.path("name").asText())) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode read(String name) throws IOException {
        try (InputStream stream = OpenCliStructuredHelpFixtureTest.class.getResourceAsStream(FIXTURES + name)) {
            assertNotNull(stream, name);
            return STRICT_JSON.readTree(stream);
        }
    }
}
