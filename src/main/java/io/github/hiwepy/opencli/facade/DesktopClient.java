package io.github.hiwepy.opencli.facade;

import io.github.hiwepy.opencli.OpenCliClient;
import io.github.hiwepy.opencli.adapter.desktop.codex.CodexOpenCliClient;
import io.github.hiwepy.opencli.adapter.desktop.cursor.CursorOpenCliClient;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import io.github.hiwepy.opencli.registry.OpenCliAdapterIds;
import lombok.Getter;

/**
 * Desktop（Electron/CDP）适配器入口。
 */
public class DesktopClient {

    @Getter
    private final OpenCliClient openCli;

    public DesktopClient(OpenCliClient openCli) {
        this.openCli = openCli;
    }

    public CodexOpenCliClient codex() {
        return openCli.codex();
    }

    public CursorOpenCliClient cursor() {
        return openCli.cursor();
    }

    public OpenCliAdapterChannel antigravity() {
        return openCli.adapter(OpenCliAdapterIds.ANTIGRAVITY);
    }

    public OpenCliAdapterChannel chatgptApp() {
        return openCli.adapter(OpenCliAdapterIds.CHATGPT_APP);
    }

    public OpenCliAdapterChannel chatwise() {
        return openCli.adapter(OpenCliAdapterIds.CHATWISE);
    }

    public OpenCliAdapterChannel discordApp() {
        return openCli.adapter(OpenCliAdapterIds.DISCORD_APP);
    }

    /** @deprecated 请使用 {@link #discordApp()}；CLI site 为 {@code discord-app}。 */
    @Deprecated
    public OpenCliAdapterChannel discord() {
        return discordApp();
    }

    public OpenCliAdapterChannel doubaoApp() {
        return openCli.adapter(OpenCliAdapterIds.DOUBAO_APP);
    }
}
