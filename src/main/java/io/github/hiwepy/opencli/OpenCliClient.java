package io.github.hiwepy.opencli;

import io.github.hiwepy.opencli.adapter.browser.chatgpt.ChatgptOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.claude.ClaudeOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.gemini.GeminiOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.jimeng.JimengOpenCliClient;
import io.github.hiwepy.opencli.adapter.desktop.codex.CodexOpenCliClient;
import io.github.hiwepy.opencli.adapter.desktop.cursor.CursorOpenCliClient;
import io.github.hiwepy.opencli.adapter.publicapi.arxiv.ArxivOpenCliClient;
import io.github.hiwepy.opencli.adapter.publicapi.binance.BinanceOpenCliClient;
import io.github.hiwepy.opencli.adapter.publicapi.npm.NpmOpenCliClient;
import io.github.hiwepy.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.facade.BrowserClient;
import io.github.hiwepy.opencli.facade.DesktopClient;
import io.github.hiwepy.opencli.facade.PublicApiClient;
import java.util.Objects;
import lombok.Getter;

/**
 * OpenCLI Java SDK 入口：持有一组运行时配置与共享 {@link OpenCliExecutor}。
 */
public class OpenCliClient {

    @Getter
    private final OpenCliProperties properties;

    @Getter
    private final OpenCliExecutor executor;

    /**
     * 使用默认配置构造（可执行文件假定名为 {@code opencli}）。
     */
    public OpenCliClient() {
        this(new OpenCliProperties());
    }

    /**
     * @param properties 运行配置，不得为 null
     */
    public OpenCliClient(OpenCliProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.executor = new OpenCliExecutor(properties);
    }

    /**
     * 使用外部已注册的 {@link OpenCliExecutor}（例如 Spring 单例 Bean），避免重复构造执行器。
     *
     * @param properties 与 executor 一致的运行配置，不得为 null
     * @param executor   共享执行器，不得为 null
     */
    public OpenCliClient(OpenCliProperties properties, OpenCliExecutor executor) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * 绑定任意 adapter id，用于文档中其余 100+ 适配器的通用调用。
     *
     * @param adapterId 如 {@code twitter}；需与 {@code opencli list} 一致
     * @return 通道对象
     */
    public OpenCliAdapterChannel adapter(String adapterId) {
        return new OpenCliAdapterChannel(executor, adapterId);
    }

    // ----- 参考强类型门面（Desktop） -----

    /** @return Codex 桌面适配器客户端 */
    public CodexOpenCliClient codex() {
        return new CodexOpenCliClient(executor);
    }

    /** @return Cursor 桌面适配器客户端 */
    public CursorOpenCliClient cursor() {
        return new CursorOpenCliClient(executor);
    }

    // ----- 参考强类型门面（Browser LLM / 即梦） -----

    /** @return Gemini 浏览器适配器客户端 */
    public GeminiOpenCliClient gemini() {
        return new GeminiOpenCliClient(executor);
    }

    /** @return Claude 浏览器适配器客户端 */
    public ClaudeOpenCliClient claude() {
        return new ClaudeOpenCliClient(executor);
    }

    /** @return ChatGPT Web 适配器客户端 */
    public ChatgptOpenCliClient chatgpt() {
        return new ChatgptOpenCliClient(executor);
    }

    /** @return 即梦 Jimeng 适配器客户端 */
    public JimengOpenCliClient jimeng() {
        return new JimengOpenCliClient(executor);
    }

    // ----- 参考强类型门面（Public API） -----

    /** @return arXiv 公共 API 客户端 */
    public ArxivOpenCliClient arxiv() {
        return new ArxivOpenCliClient(executor);
    }

    /** @return npm registry 客户端 */
    public NpmOpenCliClient npm() {
        return new NpmOpenCliClient(executor);
    }

    /** @return Binance 公共行情客户端 */
    public BinanceOpenCliClient binance() {
        return new BinanceOpenCliClient(executor);
    }

    /** @return Wikipedia 公共 API 客户端 */
    public WikipediaOpenCliClient wikipedia() {
        return new WikipediaOpenCliClient(executor);
    }

    /** @return 公共 API 分类门面 */
    public PublicApiClient publicApis() {
        return new PublicApiClient(this);
    }

    /** @return 浏览器适配器分类门面 */
    public BrowserClient browsers() {
        return new BrowserClient(this);
    }

    /** @return 桌面（CDP）适配器分类门面 */
    public DesktopClient desktops() {
        return new DesktopClient(this);
    }
}
