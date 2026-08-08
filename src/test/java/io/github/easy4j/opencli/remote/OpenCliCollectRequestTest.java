package io.github.easy4j.opencli.remote;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliCollectRequestTest {

    @Test
    void shouldBuildWithDefaults() {
        OpenCliCollectRequest req = OpenCliCollectRequest.builder().build();
        assertEquals("json", req.getFormat());
        assertEquals("cdp", req.getMode());
        assertNotNull(req.getArgs());
        assertNotNull(req.getPositionalArgs());
    }

    @Test
    void shouldBuildWithAllFields() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("timeout", "30");
        List<String> positional = Arrays.asList("arg1");
        OpenCliCollectRequest req = OpenCliCollectRequest.builder()
            .site("chatgpt")
            .command("ask")
            .format("yaml")
            .mode("bridge")
            .cdpEndpoint("ws://cdp")
            .args(args)
            .positionalArgs(positional)
            .build();
        assertEquals("chatgpt", req.getSite());
        assertEquals("ask", req.getCommand());
        assertEquals("yaml", req.getFormat());
        assertEquals("bridge", req.getMode());
        assertEquals("ws://cdp", req.getCdpEndpoint());
        assertEquals("30", req.getArgs().get("timeout"));
        assertEquals(Arrays.asList("arg1"), req.getPositionalArgs());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        OpenCliCollectRequest req = new OpenCliCollectRequest();
        assertNotNull(req);
    }
}
