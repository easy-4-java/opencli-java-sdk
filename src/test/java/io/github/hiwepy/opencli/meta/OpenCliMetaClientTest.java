package io.github.hiwepy.opencli.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenCliMetaClientTest {

    @Test
    void listJsonFormat() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).list("json");
        assertEquals(List.of("list", "-f", "json"), exec.lastInvocation());
    }

    @Test
    void conventionAuditStrict() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).conventionAudit("twitter", null, "json", true);
        List<String> argv = exec.lastInvocation();
        assertEquals("convention-audit", argv.get(0));
        assertEquals("twitter", argv.get(1));
        assertTrue(argv.contains("--strict"));
        assertTrue(argv.contains("json"));
    }

    @Test
    void pluginInstall() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).plugin().install("github:user/repo");
        assertEquals(List.of("plugin", "install", "github:user/repo"), exec.lastInvocation());
    }
}
