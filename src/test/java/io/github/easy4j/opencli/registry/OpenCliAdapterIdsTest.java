package io.github.easy4j.opencli.registry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliAdapterIdsTest {

    @Test
    void shouldContainCommonAdapterIds() {
        assertEquals("chatgpt", OpenCliAdapterIds.CHATGPT);
        assertEquals("claude", OpenCliAdapterIds.CLAUDE);
        assertEquals("deepseek", OpenCliAdapterIds.DEEPSEEK);
        assertEquals("gemini", OpenCliAdapterIds.GEMINI);
        assertEquals("codex", OpenCliAdapterIds.CODEX);
        assertEquals("cursor", OpenCliAdapterIds.CURSOR);
        assertEquals("arxiv", OpenCliAdapterIds.ARXIV);
        assertEquals("npm", OpenCliAdapterIds.NPM);
        assertEquals("pypi", OpenCliAdapterIds.PYPI);
        assertEquals("binance", OpenCliAdapterIds.BINANCE);
        assertEquals("wikipedia", OpenCliAdapterIds.WIKIPEDIA);
        assertEquals("jimeng", OpenCliAdapterIds.JIMENG);
    }

    @Test
    void shouldHaveNonEmptyAllArray() {
        assertTrue(OpenCliAdapterIds.ALL.length > 0);
    }

    @Test
    void allArrayShouldContainChatgpt() {
        boolean found = false;
        for (String id : OpenCliAdapterIds.ALL) {
            if ("chatgpt".equals(id)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void shouldHaveAdapterCounts() {
        assertTrue(OpenCliAdapterIds.BROWSER_ADAPTER_COUNT > 0);
        assertTrue(OpenCliAdapterIds.DESKTOP_ADAPTER_COUNT > 0);
        assertTrue(OpenCliAdapterIds.TOTAL_ADAPTER_COUNT > 0);
    }
}
