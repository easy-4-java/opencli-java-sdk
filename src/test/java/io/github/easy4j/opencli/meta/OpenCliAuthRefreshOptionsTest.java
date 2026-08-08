package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliAuthRefreshOptionsTest {

    @Test
    void shouldBuildWithOptions() {
        OpenCliAuthRefreshOptions opts = OpenCliAuthRefreshOptions.builder()
            .site("chatgpt")
            .all(true)
            .concurrency(4)
            .timeout(30)
            .format("json")
            .build();
        assertEquals("chatgpt", opts.getSite());
        assertTrue(opts.getAll());
        assertEquals(4, opts.getConcurrency());
        assertEquals(30, opts.getTimeout());
        assertEquals("json", opts.getFormat());
    }
}
