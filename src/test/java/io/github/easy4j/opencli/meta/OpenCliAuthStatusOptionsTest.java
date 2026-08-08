package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliAuthStatusOptionsTest {

    @Test
    void shouldBuildWithOptions() {
        OpenCliAuthStatusOptions opts = OpenCliAuthStatusOptions.builder()
            .site("chatgpt")
            .full(true)
            .concurrency(4)
            .timeout(30)
            .only("expired")
            .format("json")
            .build();
        assertEquals("chatgpt", opts.getSite());
        assertTrue(opts.getFull());
        assertEquals(4, opts.getConcurrency());
        assertEquals(30, opts.getTimeout());
        assertEquals("expired", opts.getOnly());
        assertEquals("json", opts.getFormat());
    }
}
