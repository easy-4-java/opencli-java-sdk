package io.github.easy4j.opencli.browser.support;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 浏览器子命令的语义定位参数（对齐 CLI {@code --role}/{@code --name}/{@code --label}/{@code --text}/{@code --testid}）。
 */
@Data
@Builder/**

 * Semantic locator parameters for browser subcommands, aligned with CLI flags
 * {@code --role}, {@code --name}, {@code --label}, {@code --text}, and {@code --testid}.

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public class OpenCliBrowserSemanticLocator {

    private String role;
    private String name;
    private String label;
    private String text;
    private String testId;

    /**
     * 将语义定位 flag 追加到 argv。
     *
     * @param target 可变 argv 列表
     */
    public void appendTo(List<String> target) {
        OpenCliArgSupport.addOptionPairIfPresent(target, "--role", role);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--name", name);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--label", label);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--text", text);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--testid", testId);
    }
}
