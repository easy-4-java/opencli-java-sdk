package io.github.easy4j.opencli.adapter.publicapi.pypi;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PypiDownloadPeriodTest {

    @Test
    void shouldHaveTwoValues() {
        assertEquals(2, PypiDownloadPeriod.values().length);
    }

    @Test
    void shouldHaveCorrectCliValues() {
        assertEquals("recent", PypiDownloadPeriod.RECENT.getCliValue());
        assertEquals("overall", PypiDownloadPeriod.OVERALL.getCliValue());
    }
}
