package io.github.easy4j.opencli.facade;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.adapter.desktop.codex.CodexOpenCliClient;
import io.github.easy4j.opencli.adapter.desktop.cursor.CursorOpenCliClient;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.registry.OpenCliAdapterIds;
import lombok.Getter;

/**
 * Desktop (Electron/CDP) adapter entry point.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public class DesktopClient {

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

    public OpenCliAdapterChannel qoder() {
        return openCli.adapter(OpenCliAdapterIds.QODER);
    }

    public OpenCliAdapterChannel traeCn() {
        return openCli.adapter(OpenCliAdapterIds.TRAE_CN);
    }

    public OpenCliAdapterChannel traeSolo() {
        return openCli.adapter(OpenCliAdapterIds.TRAE_SOLO);
    }
}
