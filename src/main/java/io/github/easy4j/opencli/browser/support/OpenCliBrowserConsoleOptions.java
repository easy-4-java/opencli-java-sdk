package io.github.easy4j.opencli.browser.support;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * {@code browser console} 选项，对齐 OpenCLI {@code --level}、{@code --since}、{@code --until}、{@code --follow}。
 */
@Data
@Builder/**

 * Options for {@code browser console}, aligned with OpenCLI {@code --level},
 * {@code --since}, {@code --until}, and {@code --follow} flags.

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class OpenCliBrowserConsoleOptions {

    /** 级别：all、error、warning、log、info、debug（CLI 默认 all）。 */
    private String level;

    /** 仅包含最近 duration 内的消息，如 {@code 30s}、{@code 2m}。 */
    private String since;

    /** 仅包含早于 duration 的消息。 */
    private String until;

    /** 是否持续输出新 console 消息（JSON lines）。 */
    private Boolean follow;

    /**
     * 追加到 argv。
     *
     * @param target 可变 argv 列表
     */
    public void appendTo(List<String> target) {
        OpenCliArgSupport.addOptionPairIfPresent(target, "--level", level);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--since", since);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--until", until);
        OpenCliArgSupport.addFlagIfTrue(target, "--follow", follow);
    }
}
