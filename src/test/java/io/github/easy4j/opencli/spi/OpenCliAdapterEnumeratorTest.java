package io.github.easy4j.opencli.spi;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.registry.OpenCliAdapterIds;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OpenCliAdapterEnumeratorTest {

    @Test
    void shouldIterateAllAdapterIds() {
        AtomicInteger count = new AtomicInteger();
        OpenCliAdapterEnumerator.forEachAdapterId(id -> {
            assertNotNull(id);
            count.incrementAndGet();
        });
        assertEquals(OpenCliAdapterIds.ALL.length, count.get());
    }

    @Test
    void shouldIterateAllChannels() {
        OpenCliClient client = new OpenCliClient();
        AtomicInteger count = new AtomicInteger();
        OpenCliAdapterEnumerator.forEachChannel(
            id -> client.adapter(id),
            OpenCliAdapterChannel::getAdapterId,
            id -> count.incrementAndGet()
        );
        assertEquals(OpenCliAdapterIds.ALL.length, count.get());
    }
}
