package io.github.hiwepy.opencli.browser.support;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * {@code browser get html} 选项，对齐 OpenCLI {@code --selector}、{@code --as}、{@code --max} 等。
 */
@Data
@Builder
public final class OpenCliBrowserGetHtmlOptions {

    private String selector;

    /** 输出格式：{@code html}（默认）或 {@code json}。 */
    private String as;

    /** 原始 HTML 最大字符数（0 表示不限制）。 */
    private Integer max;

    /** {@code --as json} 时树的最大深度。 */
    private Integer depth;

    /** {@code --as json} 时每节点最大子元素数。 */
    private Integer childrenMax;

    /** {@code --as json} 时每节点 direct text 最大字符数。 */
    private Integer textMax;

    /**
     * 追加到 argv。
     *
     * @param target 可变 argv 列表
     */
    public void appendTo(List<String> target) {
        if (selector != null) {
            OpenCliArgSupport.addOptionPair(target, "--selector", selector);
        }
        if (as != null) {
            OpenCliArgSupport.addOptionPair(target, "--as", as);
        }
        if (max != null) {
            OpenCliArgSupport.addOptionPair(target, "--max", String.valueOf(max));
        }
        if (depth != null) {
            OpenCliArgSupport.addOptionPair(target, "--depth", String.valueOf(depth));
        }
        if (childrenMax != null) {
            OpenCliArgSupport.addOptionPair(target, "--children-max", String.valueOf(childrenMax));
        }
        if (textMax != null) {
            OpenCliArgSupport.addOptionPair(target, "--text-max", String.valueOf(textMax));
        }
    }
}
