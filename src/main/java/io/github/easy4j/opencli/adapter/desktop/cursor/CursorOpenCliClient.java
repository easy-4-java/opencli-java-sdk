package io.github.easy4j.opencli.adapter.desktop.cursor;

import io.github.easy4j.opencli.util.OpenCliLists;
import io.github.easy4j.opencli.adapter.desktop.support.DesktopThreadSelection;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.registry.OpenCliAdapterIds;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code cursor} 桌面适配器封装。
 */
@RequiredArgsConstructor/**

 * OpenCLI {@code cursor} desktop adapter client.
 *
 * <p>Provides typed methods for Cursor desktop interactions such as {@code send}, {@code ask},
 * {@code read}, {@code composer}, {@code model}, {@code history}, and conversation export.</p>

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

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
        return screenshot(null);
    }

    /**
     * 截图与快照制品。
     *
     * @param output 输出路径（{@code --output}），可为 null
     */
    public OpenCliResult screenshot(String output) {
        List<String> args = new ArrayList<>();
        args.add("screenshot");
        if (output != null) {
            OpenCliArgSupport.addOptionPair(args, "--output", output);
        }
        return ch().invoke(args);
    }

    /** 新建文件/标签（文档：{@code Cmd+N}）。 */
    public OpenCliResult newTab(List<String> additionalRawArgs) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("new"), additionalRawArgs));
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
        return model(null);
    }

    /**
     * 读取或切换模型。
     *
     * @param modelName 模型 ID（positional {@code model-name}，如 {@code claude-3.5-sonnet}），可为 null 表示仅读取
     */
    public OpenCliResult model(String modelName) {
        List<String> args = new ArrayList<>();
        args.add("model");
        if (modelName != null) {
            args.add(modelName);
        }
        return ch().invoke(args);
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
        return exportConversation(null);
    }

    /**
     * 导出 Markdown。
     *
     * @param output 输出路径（{@code --output}），可为 null
     */
    public OpenCliResult exportConversation(String output) {
        List<String> args = new ArrayList<>();
        args.add("export");
        if (output != null) {
            OpenCliArgSupport.addOptionPair(args, "--output", output);
        }
        return ch().invoke(args);
    }
}
