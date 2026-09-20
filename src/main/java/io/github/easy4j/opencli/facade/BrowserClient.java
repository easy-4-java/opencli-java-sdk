package io.github.easy4j.opencli.facade;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.adapter.browser.chatgpt.ChatgptOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.claude.ClaudeOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.deepseek.DeepseekOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.doubao.DoubaoOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.gemini.GeminiOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.grok.GrokOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.jimeng.JimengOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.kimi.KimiOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.qwen.QwenOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.yuanbao.YuanbaoOpenCliClient;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import lombok.Getter;

/**
 * Browser-type adapter entry point: wraps {@link OpenCliClient} and exposes
 * browser/Bridge facades.
 *
 * <p>For adapters without a dedicated typed client, use {@link OpenCliClient#adapter(String)}
 * or {@link io.github.easy4j.opencli.registry.OpenCliAdapterIds} constants to assemble subcommands.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public class BrowserClient {

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

    public DeepseekOpenCliClient deepseek() {
        return openCli.deepseek();
    }

    public KimiOpenCliClient kimi() {
        return openCli.kimi();
    }

    public QwenOpenCliClient qwen() {
        return openCli.qwen();
    }

    public DoubaoOpenCliClient doubao() {
        return openCli.doubao();
    }

    public GrokOpenCliClient grok() {
        return openCli.grok();
    }

    public YuanbaoOpenCliClient yuanbao() {
        return openCli.yuanbao();
    }
}
