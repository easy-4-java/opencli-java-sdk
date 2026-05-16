package io.github.hiwepy.opencli.adapter.browser.support;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 浏览器侧 LLM 适配器常用的超时、是否新开会话、JSON 输出等选项。
 */
@Data
@Builder
public class BrowserLlmOptions {

    private Integer timeoutSeconds;

    /**
     * 部分适配器使用 {@code --new true} 形态。
     */
    private Boolean startNewChat;

    /**
     * 例如 Gemini 的 {@code --site-session ephemeral}。
     */
    private String siteSession;

    private Boolean jsonOutput;

    /**
     * @param target argv 列表
     */
    public void appendTo(List<String> target) {
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(target, "--timeout", String.valueOf(timeoutSeconds));
        }
        if (Boolean.TRUE.equals(startNewChat)) {
            OpenCliArgSupport.addOptionPair(target, "--new", "true");
        }
        if (siteSession != null && !siteSession.isEmpty()) {
            OpenCliArgSupport.addOptionPair(target, "--site-session", siteSession);
        }
        if (Boolean.TRUE.equals(jsonOutput)) {
            target.add("-f");
            target.add("json");
        }
    }
}
