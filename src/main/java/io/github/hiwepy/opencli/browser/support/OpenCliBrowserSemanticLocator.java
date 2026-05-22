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
        if (role != null) {
            OpenCliArgSupport.addOptionPair(target, "--role", role);
        }
        if (name != null) {
            OpenCliArgSupport.addOptionPair(target, "--name", name);
        }
        if (label != null) {
            OpenCliArgSupport.addOptionPair(target, "--label", label);
        }
        if (text != null) {
            OpenCliArgSupport.addOptionPair(target, "--text", text);
        }
        if (testId != null) {
            OpenCliArgSupport.addOptionPair(target, "--testid", testId);
        }
    }
}
