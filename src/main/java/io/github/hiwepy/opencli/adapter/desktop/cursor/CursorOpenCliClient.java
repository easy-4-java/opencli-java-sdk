package io.github.hiwepy.opencli.adapter.desktop.cursor;

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
 * OpenCLI {@code cursor} 桌面适配器封装。
 */
@RequiredArgsConstructor
public final class CursorOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.CURSOR);
    }

    /** CDP 与页面诊断。 */
    public OpenCliResult status() {
        return ch().invoke("status");
    }

    /** DOM / Accessibility 导出。 */
    public OpenCliResult dump() {
        return ch().invoke("dump");
    }

    /** 截图与快照制品。 */
    public OpenCliResult screenshot() {
        return ch().invoke("screenshot");
    }

    /** 新建文件/标签（文档：{@code Cmd+N}）。 */
    public OpenCliResult newTab(List<String> additionalRawArgs) {
        return ch().invoke(OpenCliArgSupport.merge(List.of("new"), additionalRawArgs));
    }

    /** 同 {@link #newTab(List)}。 */
    public OpenCliResult newTab() {
        return newTab(null);
    }

    /**
     * 向当前 Composer 注入消息并提交。
     *
     * @param message           文本内容
     * @param selection         可选工程/会话选择（若 Cursor 子命令支持相同 flag）
     * @param additionalRawArgs 透传参数
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

    /** {@code ask}：发送并等待。 */
    public OpenCliResult ask(String message, DesktopThreadSelection selection, List<String> additionalRawArgs) {
        List<String> args = new ArrayList<>();
        args.add("ask");
        args.add(message);
        if (selection != null) {
            selection.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, additionalRawArgs));
    }

    /** 读取侧栏会话全文。 */
    public OpenCliResult read(DesktopThreadSelection selection, List<String> additionalRawArgs) {
        List<String> args = new ArrayList<>();
        args.add("read");
        if (selection != null) {
            selection.appendTo(args);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, additionalRawArgs));
    }

    /** 无选择的 {@link #read(DesktopThreadSelection, List)}。 */
    public OpenCliResult read() {
        return read(null, null);
    }

    /**
     * 打开 Composer 并发送内联编辑提示。
     *
     * @param prompt            Composer 提示文本
     * @param additionalRawArgs 透传参数
     */
    public OpenCliResult composer(String prompt, List<String> additionalRawArgs) {
        List<String> args = new ArrayList<>();
        args.add("composer");
        args.add(prompt);
        return ch().invoke(OpenCliArgSupport.merge(args, additionalRawArgs));
    }

    /** 当前模型。 */
    public OpenCliResult model() {
        return ch().invoke("model");
    }

    /** 抽取对话中的代码块。 */
    public OpenCliResult extractCode() {
        return ch().invoke("extract-code");
    }

    /** 侧栏会话历史。 */
    public OpenCliResult history() {
        return ch().invoke("history");
    }

    /** 导出 Markdown。 */
    public OpenCliResult exportConversation() {
        return ch().invoke("export");
    }
}
