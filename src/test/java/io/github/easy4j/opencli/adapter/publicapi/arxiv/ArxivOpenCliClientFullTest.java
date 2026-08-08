package io.github.easy4j.opencli.adapter.publicapi.arxiv;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import org.junit.jupiter.api.Test;

class ArxivOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final ArxivOpenCliClient client = new ArxivOpenCliClient(exec);

    @Test void shouldCallSearch() { assertNotNull(client.search("AI", 10, false, null)); }
    @Test void shouldCallSearchJson() { assertNotNull(client.search("AI", 10, true, null)); }
    @Test void shouldCallPaper() { assertNotNull(client.paper("2301.00001", false, null)); }
    @Test void shouldCallPaperJson() { assertNotNull(client.paper("2301.00001", true, null)); }
    @Test void shouldCallRecent() { assertNotNull(client.recent("cs.AI", 10, false, null)); }
    @Test void shouldCallRecentJson() { assertNotNull(client.recent("cs.AI", 10, true, null)); }
    @Test void shouldCallAuthor() { assertNotNull(client.author("Smith", 10, false, null)); }
    @Test void shouldCallAuthorJson() { assertNotNull(client.author("Smith", 10, true, null)); }
    @Test void shouldCallSearchTyped() { assertNotNull(client.searchTyped("AI", 10, null)); }
    @Test void shouldCallPaperTyped() { assertNotNull(client.paperTyped("2301.00001", null)); }
    @Test void shouldCallRecentTyped() { assertNotNull(client.recentTyped("cs.AI", 10, null)); }
}
