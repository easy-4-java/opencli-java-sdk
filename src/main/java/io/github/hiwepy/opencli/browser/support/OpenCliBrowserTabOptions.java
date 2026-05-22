package io.github.hiwepy.opencli.browser.support;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 浏览器子命令共用的 {@code --tab <targetId>} 选项。
 */
@Data
@Builder
public class OpenCliBrowserTabOptions {

    /** 目标 tab/page ID（来自 tab list / open / tab new）。 */
    private String tab;

    /**
     * @param target argv
     */
    public void appendTo(List<String> target) {
        OpenCliArgSupport.addOptionPairIfPresent(target, "--tab", tab);
    }
}
