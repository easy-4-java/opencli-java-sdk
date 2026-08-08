package io.github.easy4j.opencli.adapter.publicapi.wikipedia;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import org.junit.jupiter.api.Test;

class WikipediaOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final WikipediaOpenCliClient client = new WikipediaOpenCliClient(exec);

    @Test void shouldCallSearch() { assertNotNull(client.search("Java", 10, "en", false, null)); }
    @Test void shouldCallSearchJson() { assertNotNull(client.search("Java", 10, "en", true, null)); }
    @Test void shouldCallSummary() { assertNotNull(client.summary("Java", "en", null)); }
    @Test void shouldCallSummaryNullLang() { assertNotNull(client.summary("Java", null, null)); }
    @Test void shouldCallRandom() { assertNotNull(client.random("en", null)); }
    @Test void shouldCallRandomNoLang() { assertNotNull(client.random(null)); }
    @Test void shouldCallTrending() { assertNotNull(client.trending(10, "en", null)); }
    @Test void shouldCallTrendingNoArgs() { assertNotNull(client.trending(null)); }
    @Test void shouldCallPage() { assertNotNull(client.page("Java", "en", 3, null)); }
    @Test void shouldCallPageNullArgs() { assertNotNull(client.page("Java", null, null, null)); }
    @Test void shouldCallSearchTyped() { assertNotNull(client.searchTyped("Java", 10, "en", null)); }
    @Test void shouldCallPageTyped() { assertNotNull(client.pageTyped("Java", "en", 3, null)); }
    @Test void shouldCallPageTypedNullArgs() { assertNotNull(client.pageTyped("Java", null, null, null)); }
    @Test void shouldCallTrendingTyped() { assertNotNull(client.trendingTyped(10, "en", null)); }
}
