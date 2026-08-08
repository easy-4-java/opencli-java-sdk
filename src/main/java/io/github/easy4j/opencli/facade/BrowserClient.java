package io.github.easy4j.opencli.facade;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.adapter.browser.chatgpt.ChatgptOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.claude.ClaudeOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.deepseek.DeepseekOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.gemini.GeminiOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.jimeng.JimengOpenCliClient;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import lombok.Getter;

/**
 * Browser-type adapter entry point: wraps {@link OpenCliClient} and exposes
 * browser/Bridge facades.
 *
 * <p>For adapters without a dedicated typed client, use {@link OpenCliClient#adapter(String)}
 * or {@link io.github.easy4j.opencli.registry.OpenCliAdapterIds} constants to assemble subcommands.</p>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
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
}
