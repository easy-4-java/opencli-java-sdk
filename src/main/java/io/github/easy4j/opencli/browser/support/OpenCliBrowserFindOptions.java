package io.github.easy4j.opencli.browser.support;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * {@code browser find} 选项，对齐 OpenCLI {@code --css}、{@code --limit}、{@code --text-max}。
 */
@Data
@Builder(toBuilder = true)/**

 * Options for {@code browser find}, aligned with OpenCLI {@code --css},
 * {@code --limit}, and {@code --text-max} flags.

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class OpenCliBrowserFindOptions {

    private String css;

    /** 最大返回条目数（CLI 默认 50）。 */
    private Integer limit;

    /** 每条 trimmed text 最大字符数（CLI 默认 120）。 */
    private Integer textMax;

    /**
     * 追加到 argv（不含 semantic locator，由调用方单独追加）。
     *
     * @param target 可变 argv 列表
     */
    public void appendTo(List<String> target) {
        OpenCliArgSupport.addOptionPairIfPresent(target, "--css", css);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--limit", limit);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--text-max", textMax);
    }
}
