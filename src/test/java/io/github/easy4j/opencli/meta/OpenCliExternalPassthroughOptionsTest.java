package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliExternalPassthroughOptionsTest {

    @Test
    void shouldBuildWithOptions() {
        OpenCliExternalPassthroughOptions opts = OpenCliExternalPassthroughOptions.builder()
            .externalCliName("git")
            .arg("status")
            .arg("--short")
            .build();
        assertEquals("git", opts.getExternalCliName());
        assertEquals(Arrays.asList("status", "--short"), opts.getArgs());
    }
}
