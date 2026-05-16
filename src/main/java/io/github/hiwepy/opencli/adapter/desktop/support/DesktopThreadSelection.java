package io.github.hiwepy.opencli.adapter.desktop.support;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Desktop 适配器（Codex/Cursor 等）侧栏工程/会话选择相关参数的共用模型。
 * <p>
 * 语义对齐官方文档中的 {@code --project}、{@code --conversation}、{@code --index}、
 * {@code --thread-id}、{@code --timeout}。
 * </p>
 */
@Data
@Builder
public class DesktopThreadSelection {

    /**
     * 工程展示名或路径。
     */
    private String project;

    /**
     * 会话标题（可见侧栏）。
     */
    private String conversation;

    /**
     * 侧栏中会话序号（从 1 或文档约定起算，由 OpenCLI 解析）。
     */
    private Integer conversationIndex;

    /**
     * 线程 id，如 {@code local:019df125-...}。
     */
    private String threadId;

    /**
     * 等待超时（秒）。
     */
    private Integer timeoutSeconds;

    /**
     * 将已设置字段顺序追加到 argv 列表末尾。
     *
     * @param target CLI token 列表，不得为 null
     */
    public void appendTo(List<String> target) {
        if (project != null) {
            OpenCliArgSupport.addOptionPair(target, "--project", project);
        }
        if (conversation != null) {
            OpenCliArgSupport.addOptionPair(target, "--conversation", conversation);
        }
        if (conversationIndex != null) {
            OpenCliArgSupport.addOptionPair(target, "--index", String.valueOf(conversationIndex));
        }
        if (threadId != null) {
            OpenCliArgSupport.addOptionPair(target, "--thread-id", threadId);
        }
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
        }
    }
}
