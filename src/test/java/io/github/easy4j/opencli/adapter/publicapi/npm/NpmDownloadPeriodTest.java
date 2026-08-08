package io.github.easy4j.opencli.adapter.publicapi.npm;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class NpmDownloadPeriodTest {

    @Test
    void shouldHaveFourValues() {
        assertEquals(4, NpmDownloadPeriod.values().length);
    }

    @Test
    void shouldHaveCorrectCliValues() {
        assertEquals("last-day", NpmDownloadPeriod.LAST_DAY.getCliValue());
        assertEquals("last-week", NpmDownloadPeriod.LAST_WEEK.getCliValue());
        assertEquals("last-month", NpmDownloadPeriod.LAST_MONTH.getCliValue());
        assertEquals("last-year", NpmDownloadPeriod.LAST_YEAR.getCliValue());
    }
}
