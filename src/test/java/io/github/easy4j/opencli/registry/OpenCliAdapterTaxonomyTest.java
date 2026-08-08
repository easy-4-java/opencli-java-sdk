package io.github.easy4j.opencli.registry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliAdapterTaxonomyTest {

    @Test
    void shouldHaveNonEmptyDesktopIds() {
        assertFalse(OpenCliAdapterTaxonomy.DESKTOP_IDS.isEmpty());
    }

    @Test
    void shouldHaveNonEmptyBrowserIds() {
        assertFalse(OpenCliAdapterTaxonomy.BROWSER_IDS.isEmpty());
    }

    @Test
    void shouldIdentifyDesktopAdapter() {
        assertTrue(OpenCliAdapterTaxonomy.isDesktopAdapter("codex"));
        assertTrue(OpenCliAdapterTaxonomy.isDesktopAdapter("cursor"));
        assertTrue(OpenCliAdapterTaxonomy.isDesktopAdapter("chatgpt-app"));
    }

    @Test
    void shouldNotIdentifyBrowserAdapterAsDesktop() {
        assertFalse(OpenCliAdapterTaxonomy.isDesktopAdapter("chatgpt"));
        assertFalse(OpenCliAdapterTaxonomy.isDesktopAdapter("claude"));
    }

    @Test
    void shouldReturnFalseForNull() {
        assertFalse(OpenCliAdapterTaxonomy.isDesktopAdapter(null));
    }

    @Test
    void shouldReturnFalseForUnknownAdapter() {
        assertFalse(OpenCliAdapterTaxonomy.isDesktopAdapter("nonexistent"));
    }
}
