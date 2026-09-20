package io.github.easy4j.opencli.adapter.browser.support;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 浏览器侧 LLM 适配器常用的超时、是否新开会话、JSON 输出等选项。
 */
@Data
@Builder/**

 * Common options shared across browser-based LLM adapters (timeout, model selection,
 * new-chat toggle, JSON output, etc.).

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public class BrowserLlmOptions {

    private Integer timeoutSeconds;

    private String model;

    private String thinking;

    /**
     * 部分适配器使用 {@code --new} 布尔开关。
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
        if (model != null && !model.isEmpty()) {
            OpenCliArgSupport.addOptionPair(target, "--model", model);
        }
        if (thinking != null && !thinking.isEmpty()) {
            OpenCliArgSupport.addOptionPair(target, "--thinking", thinking);
        }
        if (Boolean.TRUE.equals(startNewChat)) {
            target.add("--new");
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
