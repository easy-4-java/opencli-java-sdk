package io.github.hiwepy.opencli.adapter.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.hiwepy.opencli.adapter.desktop.codex.CodexOpenCliClient;
import io.github.hiwepy.opencli.adapter.desktop.cursor.CursorOpenCliClient;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 校验 {@code model} 子命令使用 positional {@code model-name}（对齐 clis/codex/model.js、cursor/model.js）。
 */
class DesktopModelArgvTest {

    @Test
    void codexModelReadOmitsPositional() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new CodexOpenCliClient(exec).model();
        assertEquals(List.of("codex", "model"), exec.lastInvocation());
    }

    @Test
    void codexModelSwitchUsesPositionalNotFlag() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new CodexOpenCliClient(exec).model("gpt-4");
        List<String> argv = exec.lastInvocation();
        assertEquals(List.of("codex", "model", "gpt-4"), argv);
        assertFalse(argv.contains("--model-name"));
    }

    @Test
    void cursorModelSwitchUsesPositional() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new CursorOpenCliClient(exec).model("claude-3.5-sonnet");
        assertEquals(List.of("cursor", "model", "claude-3.5-sonnet"), exec.lastInvocation());
    }
}
