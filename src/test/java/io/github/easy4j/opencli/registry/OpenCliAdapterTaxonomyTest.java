package io.github.easy4j.opencli.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 生成类 {@link OpenCliAdapterTaxonomy} 与 {@link OpenCliAdapterIds} 一致性测试。
 */
class OpenCliAdapterTaxonomyTest {

    @Test
    void desktopAndBrowserPartitionCoversUnion() {
        assertEquals(OpenCliAdapterIds.DESKTOP_ADAPTER_COUNT, OpenCliAdapterTaxonomy.DESKTOP_IDS.size());
        assertEquals(OpenCliAdapterIds.BROWSER_ADAPTER_COUNT, OpenCliAdapterTaxonomy.BROWSER_IDS.size());
        assertEquals(OpenCliAdapterIds.TOTAL_ADAPTER_COUNT, OpenCliAdapterIds.ALL.length);
        for (String id : OpenCliAdapterTaxonomy.DESKTOP_IDS) {
            assertTrue(OpenCliAdapterTaxonomy.isDesktopAdapter(id));
        }
        assertEquals(OpenCliAdapterIds.TOTAL_ADAPTER_COUNT, OpenCliAdapterIds.ALL.length);
    }
}
