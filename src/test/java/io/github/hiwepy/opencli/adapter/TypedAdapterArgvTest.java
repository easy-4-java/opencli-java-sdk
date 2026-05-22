package io.github.hiwepy.opencli.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hiwepy.opencli.adapter.browser.gemini.GeminiOpenCliClient;
import io.github.hiwepy.opencli.adapter.publicapi.npm.NpmOpenCliClient;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 强类型 adapter argv 抽查。 */
class TypedAdapterArgvTest {

    @Test
    void geminiDeepResearchConfirmLabel() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new GeminiOpenCliClient(exec).deepResearch(
            "topic",
            GeminiOpenCliClient.GeminiDeepResearchOptions.builder()
                .confirmLabel("Start research")
                .build(),
            null);
        List<String> argv = exec.lastInvocation();
        assertEquals("gemini", argv.get(0));
        assertTrue(argv.contains("--confirm"));
        assertTrue(argv.contains("Start research"));
    }

    @Test
    void npmPackageInfoJson() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new NpmOpenCliClient(exec).packageInfo("react", true, null);
        assertEquals(List.of("npm", "package", "react", "-f", "json"), exec.lastInvocation());
    }
}
