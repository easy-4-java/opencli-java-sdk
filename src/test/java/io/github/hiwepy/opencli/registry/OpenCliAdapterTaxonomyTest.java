package io.github.hiwepy.opencli.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 生成类 {@link OpenCliAdapterTaxonomy} 与 {@link OpenCliAdapterIds} 一致性测试。
 */
class OpenCliAdapterTaxonomyTest {

    @Test
    void desktopAndBrowserPartitionCoversUnion() {
        assertEquals(7, OpenCliAdapterTaxonomy.DESKTOP_IDS.size());
        assertEquals(127, OpenCliAdapterTaxonomy.BROWSER_IDS.size());
        assertEquals(OpenCliAdapterIds.TOTAL_ADAPTER_COUNT, OpenCliAdapterIds.ALL.length);
        for (String id : OpenCliAdapterTaxonomy.DESKTOP_IDS) {
            assertTrue(OpenCliAdapterTaxonomy.isDesktopAdapter(id));
        }
        assertEquals(134, OpenCliAdapterIds.ALL.length);
    }
}
