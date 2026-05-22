package io.github.hiwepy.opencli.browser.support;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 浏览器子命令的语义定位参数（对齐 CLI {@code --role}/{@code --name}/{@code --label}/{@code --text}/{@code --testid}）。
 */
@Data
@Builder
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
