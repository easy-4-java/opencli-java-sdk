package io.github.hiwepy.opencli.adapter.desktop.codex;

import io.github.hiwepy.opencli.util.OpenCliLists;
import io.github.hiwepy.opencli.adapter.desktop.support.DesktopThreadSelection;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.registry.OpenCliAdapterIds;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code codex} 桌面适配器封装（Chrome DevTools / Electron）。
 */
@RequiredArgsConstructor
public final class CodexOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.CODEX);
    }

    /** 连接与窗口诊断。 */
    public OpenCliResult status() {
        return ch().invoke("status");
    }

    /** 将 DOM / Accessibility 树导出到临时目录。 */
    public OpenCliResult dump() {
        return ch().invoke("dump");
    }

    /** DOM + 快照截图类制品。 */
    public OpenCliResult screenshot() {
        return ch().invoke("screenshot");
    }

    /**
     * 新建独立 Git Worktree 线程上下文（文档：{@code Cmd+N} 语义）。
     *
     * @param additionalRawArgs 透传 CLI 附加参数，可为 null
     */
    public OpenCliResult newSession(List<String> additionalRawArgs) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), additionalRawArgs));
    }

    /** 同 {@link #newSession(List)}，无附加参数。 */
    public OpenCliResult newSession() {
        return newSession(null);
    }

    /**
     * {@code opencli codex send "message"}。
     *
     * @param message           发送内容，不得为空白
     * @param selection         工程/会话选择，可为 null
     * @param additionalRawArgs 额外透传参数，可为 null
     */
    public OpenCliResult send(String message, DesktopThreadSelection selection, List<String> additionalRawArgs) {
        List<String> args = new ArrayList<>();
        args.add("send");
        args.add(message);
        if (selection != null) {
            selection.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, additionalRawArgs));
    }

    /**
     * {@code opencli codex ask "message"}：发送并等待回复。
     *
     * @param message           提问内容
     * @param selection         可选侧栏定位
     * @param additionalRawArgs 透传参数
     */
    public OpenCliResult ask(String message, DesktopThreadSelection selection, List<String> additionalRawArgs) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(message);
        if (selection != null) {
            selection.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, additionalRawArgs));
    }

    /** 读取当前线程全文与推理日志。 */
    public OpenCliResult read(DesktopThreadSelection selection, List<String> additionalRawArgs) {
        List<String> args = new ArrayList<>();
        args.add("read");
        if (selection != null) {
            selection.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, additionalRawArgs));
    }

    /** 无选择条件的 {@link #read(DesktopThreadSelection, List)}。 */
    public OpenCliResult read() {
        return read(null, null);
    }

    /** 侧栏可见工程与会话。 */
    public OpenCliResult projects() {
        return ch().invoke("projects");
    }

    /** 会话历史列表。 */
    public OpenCliResult history() {
        return ch().invoke("history");
    }

    /** 抽取可视 Patch / Diff。 */
    public OpenCliResult extractDiff() {
        return ch().invoke("extract-diff");
    }

    /** 当前模型名。 */
    public OpenCliResult model() {
        return ch().invoke("model");
    }

    /** 导出当前会话为 Markdown。 */
    public OpenCliResult exportConversation() {
        return ch().invoke("export");
    }
}
