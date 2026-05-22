package io.github.hiwepy.opencli.browser.support;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * {@code browser extract} 选项，对齐 OpenCLI {@code --selector}、{@code --chunk-size}、{@code --start}。
 */
@Data
@Builder
public final class OpenCliBrowserExtractOptions {

    private String selector;

    /** 目标 chunk 字符数（CLI 默认 20000）。 */
    private Integer chunkSize;

    /** 起始字符偏移（使用上一次 extract 返回的 {@code next_start_char}）。 */
    private Integer start;

    /**
     * 追加到 argv。
     *
     * @param target 可变 argv 列表
     */
    public void appendTo(List<String> target) {
        if (selector != null) {
            OpenCliArgSupport.addOptionPair(target, "--selector", selector);
        }
        if (chunkSize != null) {
            OpenCliArgSupport.addOptionPair(target, "--chunk-size", String.valueOf(chunkSize));
        }
        if (start != null) {
            OpenCliArgSupport.addOptionPair(target, "--start", String.valueOf(start));
        }
    }
}
