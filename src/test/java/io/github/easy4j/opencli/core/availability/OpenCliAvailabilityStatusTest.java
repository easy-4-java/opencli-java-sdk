package io.github.easy4j.opencli.core.availability;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliAvailabilityStatusTest {

    @Test
    void shouldHaveAllExpectedValues() {
        OpenCliAvailabilityStatus[] values = OpenCliAvailabilityStatus.values();
        assertEquals(9, values.length);
    }

    @Test
    void shouldContainExpectedStatuses() {
        assertNotNull(OpenCliAvailabilityStatus.valueOf("AVAILABLE"));
        assertNotNull(OpenCliAvailabilityStatus.valueOf("SKIPPED_REMOTE_MODE"));
        assertNotNull(OpenCliAvailabilityStatus.valueOf("EXECUTABLE_NOT_CONFIGURED"));
        assertNotNull(OpenCliAvailabilityStatus.valueOf("EXECUTABLE_NOT_FOUND"));
        assertNotNull(OpenCliAvailabilityStatus.valueOf("EXECUTABLE_NOT_EXECUTABLE"));
        assertNotNull(OpenCliAvailabilityStatus.valueOf("SPAWN_FAILED"));
        assertNotNull(OpenCliAvailabilityStatus.valueOf("NON_ZERO_EXIT"));
        assertNotNull(OpenCliAvailabilityStatus.valueOf("TIMEOUT"));
        assertNotNull(OpenCliAvailabilityStatus.valueOf("FAILED"));
    }
}
