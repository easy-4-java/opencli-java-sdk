package io.github.hiwepy.opencli.browser.support;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * {@code browser state} 选项，对齐 OpenCLI {@code --source dom|ax} 与 {@code --compare-sources}。
 */
@Data
@Builder
public final class OpenCliBrowserStateOptions {

    /** 快照后端：{@code dom}（默认）或 {@code ax}。 */
    private String source;

    /** 是否输出 DOM 与 AX 快照指标对比。 */
    private Boolean compareSources;

    /**
     * 追加到 argv。
     *
     * @param target 可变 argv 列表
     */
    public void appendTo(List<String> target) {
        if (source != null) {
            OpenCliArgSupport.addOptionPair(target, "--source", source);
        }
        if (Boolean.TRUE.equals(compareSources)) {
            target.add("--compare-sources");
        }
    }
}
