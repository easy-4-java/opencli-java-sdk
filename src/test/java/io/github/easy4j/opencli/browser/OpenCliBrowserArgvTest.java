package io.github.easy4j.opencli.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.easy4j.opencli.browser.support.OpenCliBrowserConsoleOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserExtractOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserFindOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserGetHtmlOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserScreenshotOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserStateOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserTabOptions;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * 回归：{@link OpenCliBrowserSession} argv 与 opencli {@code cli.ts} 对齐。
 */
class OpenCliBrowserArgvTest {

    private OpenCliBrowserSession session(RecordingOpenCliExecutor exec) {
        return new OpenCliBrowserClient(exec).session("work", "background");
    }

    @Test
    void debugLogsKeepSessionIdentifiersAndUrlOutOfTheSummary() {
        RecordingOpenCliExecutor executor = new RecordingOpenCliExecutor();
        String sessionName = "session-" + UUID.randomUUID();
        String url = "https://example.test/?token=" + UUID.randomUUID();
        OpenCliBrowserSession browser = new OpenCliBrowserClient(executor).session(sessionName, null);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        PrintStream original = System.err;

        try (PrintStream capture = new PrintStream(diagnostics, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            browser.tabNew(url);
        } finally {
            System.setErr(original);
        }

        assertEquals(Arrays.asList("browser", sessionName, "tab", "new", url), executor.lastInvocation());
        String logs = diagnostics.toString(StandardCharsets.UTF_8);
        assertTrue(logs.contains("OpenCLI browser"));
        assertFalse(logs.contains(sessionName), "Session name must stay out of SDK diagnostics");
        assertFalse(logs.contains(url), "URL must stay out of SDK diagnostics");
    }

    @Test
    void windowModePrefix() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).tabList();
        assertEquals(Arrays.asList("browser", "work", "--window", "background", "tab", "list"), exec.lastInvocation());
    }

    @Test
    void waitUsesTimeoutMillis() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).waitFor("selector", ".loaded", null, 15_000L);
        assertEquals(
            Arrays.asList("browser", "work", "--window", "background", "wait", "selector", ".loaded", "--timeout", "15000"),
            exec.lastInvocation());
    }

    @Test
    void extractChunkSizeAndStart() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).extract(
            null,
            OpenCliBrowserExtractOptions.builder()
                .selector("main")
                .chunkSize(20000)
                .start(40000)
                .build());
        List<String> argv = exec.lastInvocation();
        assertTrue(argv.contains("--chunk-size"));
        assertTrue(argv.contains("20000"));
        assertTrue(argv.contains("--start"));
        assertTrue(argv.contains("40000"));
        assertFalse(argv.contains("--max-chars"));
    }

    @Test
    void findCssLimitTextMax() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).find(
            null,
            null,
            OpenCliBrowserFindOptions.builder().css(".btn").limit(10).textMax(80).build());
        List<String> argv = exec.lastInvocation();
        assertEquals("--css", argv.get(argv.indexOf("find") + 1));
        assertTrue(argv.contains("--limit"));
        assertTrue(argv.contains("--text-max"));
        assertFalse(argv.contains("--source"));
        assertFalse(argv.contains("--nth"));
    }

    @Test
    void stateSourceAndCompareSources() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).state(
            null,
            OpenCliBrowserStateOptions.builder().source("ax").compareSources(true).build());
        List<String> argv = exec.lastInvocation();
        assertTrue(argv.contains("--source"));
        assertTrue(argv.contains("ax"));
        assertTrue(argv.contains("--compare-sources"));
        assertFalse(argv.contains("--json"));
    }

    @Test
    void consoleLevelSinceUntilFollow() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).console(
            null,
            OpenCliBrowserConsoleOptions.builder()
                .level("error")
                .since("30s")
                .until("2m")
                .follow(true)
                .build());
        List<String> argv = exec.lastInvocation();
        assertTrue(argv.contains("--level"));
        assertTrue(argv.contains("--since"));
        assertTrue(argv.contains("--until"));
        assertTrue(argv.contains("--follow"));
        assertFalse(argv.contains("--limit"));
    }

    @Test
    void screenshotWidthHeight() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).screenshot(
            "/tmp/s.png",
            null,
            OpenCliBrowserScreenshotOptions.builder().width(1280).height(720).fullPage(true).build());
        List<String> argv = exec.lastInvocation();
        assertTrue(argv.contains("--width"));
        assertTrue(argv.contains("1280"));
        assertTrue(argv.contains("--height"));
        assertTrue(argv.contains("720"));
    }

    @Test
    void getHtmlSelectorAndBudgets() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).getHtml(
            null,
            OpenCliBrowserGetHtmlOptions.builder()
                .selector("#app")
                .as("json")
                .max(5000)
                .depth(2)
                .childrenMax(10)
                .textMax(120)
                .build());
        List<String> argv = exec.lastInvocation();
        assertEquals(Arrays.asList("browser", "work", "--window", "background", "get", "html"), argv.subList(0, 6));
        assertTrue(argv.contains("--selector"));
        assertTrue(argv.contains("--children-max"));
        assertTrue(argv.contains("--text-max"));
    }

    @Test
    void clickNth() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        session(exec).click("button.primary", null, OpenCliBrowserTabOptions.builder().tab("t1").build(), 2);
        List<String> argv = exec.lastInvocation();
        assertTrue(argv.contains("--nth"));
        assertTrue(argv.contains("2"));
        assertTrue(argv.contains("--tab"));
    }

    @Test
    void sessionOpenBuildsArgv() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliBrowserClient(exec)
            .session("work")
            .open("https://example.com", OpenCliBrowserTabOptions.builder().tab("tab-1").build());
        assertEquals(
            Arrays.asList("browser", "work", "open", "https://example.com", "--tab", "tab-1"),
            exec.lastInvocation());
    }

    @Test
    void sessionDialogDismiss() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliBrowserClient(exec).session("s1").dialogDismiss(null);
        assertEquals(Arrays.asList("browser", "s1", "dialog", "dismiss"), exec.lastInvocation());
    }
}
