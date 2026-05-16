package io.github.hiwepy.opencli.facade;

import io.github.hiwepy.opencli.OpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.chatgpt.ChatgptOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.claude.ClaudeOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.gemini.GeminiOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.jimeng.JimengOpenCliClient;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import lombok.Getter;

/**
 * Browser 类适配器入口：包装 {@link OpenCliClient}，集中暴露浏览器/Bridge 型 facade。
 * <p>
 * 未单独建模的适配器请使用 {@link OpenCliClient#adapter(String)} 或
 * {@link io.github.hiwepy.opencli.registry.OpenCliAdapterIds} 常量自行拼接子命令。
 * </p>
 */
public class BrowserClient {

    @Getter
    private final OpenCliClient openCli;

    public BrowserClient(OpenCliClient openCli) {
        this.openCli = openCli;
    }

    /** @return 任意 browser adapter 原始通道 */
    public OpenCliAdapterChannel channel(String adapterId) {
        return openCli.adapter(adapterId);
    }

    public GeminiOpenCliClient gemini() {
        return openCli.gemini();
    }

    public ClaudeOpenCliClient claude() {
        return openCli.claude();
    }

    public ChatgptOpenCliClient chatgpt() {
        return openCli.chatgpt();
    }

    public JimengOpenCliClient jimeng() {
        return openCli.jimeng();
    }
}
