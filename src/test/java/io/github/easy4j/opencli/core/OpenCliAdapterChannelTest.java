package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenCliAdapterChannelTest {

    @Test
    void shouldRejectNullExecutor() {
        assertThrows(NullPointerException.class, () -> new OpenCliAdapterChannel(null, "twitter"));
    }

    @Test
    void shouldRejectNullAdapterId() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(NullPointerException.class, () -> new OpenCliAdapterChannel(executor, null));
    }

    @Test
    void shouldRejectBlankAdapterId() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(IllegalArgumentException.class, () -> new OpenCliAdapterChannel(executor, "   "));
    }

    @Test
    void shouldReturnAdapterId() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliExecutor executor = new OpenCliExecutor(props);
        OpenCliAdapterChannel ch = new OpenCliAdapterChannel(executor, "twitter");
        assertEquals("twitter", ch.getAdapterId());
    }

    @Test
    void shouldAcceptVarargsInvoke() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliExecutor executor = new OpenCliExecutor(props);
        OpenCliAdapterChannel ch = new OpenCliAdapterChannel(executor, "twitter");
        assertThrows(Exception.class, () -> ch.invoke("ask", "hello"));
    }

    @Test
    void shouldAcceptCommandRequest() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliExecutor executor = new OpenCliExecutor(props);
        OpenCliAdapterChannel ch = new OpenCliAdapterChannel(executor, "twitter");
        OpenCliAdapterCommandRequest req = OpenCliAdapterCommandRequest.builder()
            .subcommand("ask")
            .positional("hello")
            .build();
        assertThrows(Exception.class, () -> ch.invoke(req));
    }
}
