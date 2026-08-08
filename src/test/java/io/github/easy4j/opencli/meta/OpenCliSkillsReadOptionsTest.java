package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliSkillsReadOptionsTest {

    @Test
    void shouldBuildWithOptions() {
        OpenCliSkillsReadOptions opts = OpenCliSkillsReadOptions.builder()
            .skill("opencli-browse")
            .path("README.md")
            .asJson(true)
            .build();
        assertEquals("opencli-browse", opts.getSkill());
        assertEquals("README.md", opts.getPath());
        assertTrue(opts.getAsJson());
    }
}
