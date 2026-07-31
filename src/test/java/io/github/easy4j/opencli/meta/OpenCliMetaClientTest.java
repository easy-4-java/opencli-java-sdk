package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OpenCliMetaClientTest {

    @Test
    void listJsonFormat() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).list("json");
        assertEquals(Arrays.asList("list", "-f", "json"), exec.lastInvocation());
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
        assertEquals(Arrays.asList("plugin", "install", "github:user/repo"), exec.lastInvocation());
    }

    @Test
    void completionBash() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).completion("bash");
        assertEquals(Arrays.asList("completion", "bash"), exec.lastInvocation());
    }

    @Test
    void daemonStatus() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).daemon().status();
        assertEquals(Arrays.asList("daemon", "status"), exec.lastInvocation());
    }

    @Test
    void profileList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).profile().list();
        assertEquals(Arrays.asList("profile", "list"), exec.lastInvocation());
    }

    @Test
    void adapterStatus() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).adapter().status();
        assertEquals(Arrays.asList("adapter", "status"), exec.lastInvocation());
    }

    @Test
    void externalList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliMetaClient(exec).external().list("json");
        assertEquals(Arrays.asList("external", "list", "-f", "json"), exec.lastInvocation());
    }
}
