package io.github.easy4j.opencli.browser.support;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
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
        OpenCliArgSupport.addOptionPairIfPresent(target, "--selector", selector);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--as", as);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--max", max);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--depth", depth);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--children-max", childrenMax);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--text-max", textMax);
    }
}
