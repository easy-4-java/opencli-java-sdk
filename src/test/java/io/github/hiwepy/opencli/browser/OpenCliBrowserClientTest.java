package io.github.hiwepy.opencli.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hiwepy.opencli.browser.support.OpenCliBrowserTabOptions;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenCliBrowserClientTest {

    @Test
    void sessionOpenBuildsArgv() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliBrowserClient(exec)
            .session("work")
            .open("https://example.com", OpenCliBrowserTabOptions.builder().tab("tab-1").build());
        assertEquals(
            List.of("browser", "work", "open", "https://example.com", "--tab", "tab-1"),
            exec.lastInvocation());
    }

    @Test
    void sessionTabList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliBrowserClient(exec).session("work").tabList();
        assertEquals(List.of("browser", "work", "tab", "list"), exec.lastInvocation());
    }

    @Test
    void sessionDialogDismiss() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new OpenCliBrowserClient(exec).session("s1").dialogDismiss(null);
        assertEquals(List.of("browser", "s1", "dialog", "dismiss"), exec.lastInvocation());
    }
}
