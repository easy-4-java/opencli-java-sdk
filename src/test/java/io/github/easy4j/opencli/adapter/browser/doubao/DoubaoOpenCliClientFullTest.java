package io.github.easy4j.opencli.adapter.browser.doubao;

import static org.junit.jupiter.api.Assertions.*;

import io.github.easy4j.opencli.adapter.browser.doubao.DoubaoOpenCliClient.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class DoubaoOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final DoubaoOpenCliClient client = new DoubaoOpenCliClient(exec);

    @Test
    void shouldCallAskWithPositionalText() {
        client.ask("帮我写周报", DoubaoAskOptions.builder().timeoutSeconds(30).build(), null);
        List<String> argv = exec.lastInvocation();
        assertEquals("doubao", argv.get(0));
        assertEquals("ask", argv.get(1));
        assertEquals("帮我写周报", argv.get(2));
        assertTrue(argv.contains("--timeout"));
    }

    @Test
    void shouldCallMeetingCommands() {
        client.meetingSummary("conv-1", "true", null);
        List<String> summary = exec.lastInvocation();
        assertEquals("meeting-summary", summary.get(1));
        assertEquals("conv-1", summary.get(2));
        assertTrue(summary.contains("--chapters"));

        client.meetingTranscript("conv-1", "srt", null);
        List<String> transcript = exec.lastInvocation();
        assertEquals("meeting-transcript", transcript.get(1));
        assertTrue(transcript.contains("--download"));
        assertTrue(transcript.contains("srt"));
    }

    @Test
    void shouldCallLifecycleCommands() {
        client.send("早", null);
        assertEquals("send", exec.lastInvocation().get(1));
        client.detail("conv-2", null);
        assertEquals("conv-2", exec.lastInvocation().get(2));
        client.history("5", null);
        assertTrue(exec.lastInvocation().contains("--limit"));
        client.read(null);
        assertEquals("read", exec.lastInvocation().get(1));
        client.login(30, null);
        assertEquals("login", exec.lastInvocation().get(1));
        client.whoami(null);
        assertEquals("whoami", exec.lastInvocation().get(1));
    }
}
