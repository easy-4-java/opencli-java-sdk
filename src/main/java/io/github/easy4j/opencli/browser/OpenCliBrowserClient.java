package io.github.easy4j.opencli.browser;

import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI 内置 {@code browser} 命令门面：会话优先 API {@code browser(session).open(url)}。
 */
@RequiredArgsConstructor/**

 * Facade for the built-in {@code browser} command: session-first API
 * {@code browser(session).open(url)}.

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class OpenCliBrowserClient {

    private final OpenCliExecutor executor;

    /**
     * 绑定具名浏览器会话；同一 {@code sessionName} 可跨调用保持 tab/状态。
     *
     * @param sessionName 会话名（对应 CLI 第一个 positional）
     * @return 会话作用域客户端
     */
    public OpenCliBrowserSession session(String sessionName) {
        return session(sessionName, null);
    }

    /**
     * 绑定具名浏览器会话，并指定窗口模式。
     *
     * @param sessionName 会话名
     * @param windowMode  {@code foreground} 或 {@code background}；null 表示 CLI 默认 foreground
     * @return 会话作用域客户端
     */
    public OpenCliBrowserSession session(String sessionName, String windowMode) {
        String name = Objects.requireNonNull(sessionName, "sessionName").trim();
        if (OpenCliStrings.isBlank(name)) {
            throw new IllegalArgumentException("sessionName must not be blank");
        }
        if (Objects.nonNull(windowMode)) {
            String mode = windowMode.trim().toLowerCase();
            if (!"foreground".equals(mode) && !"background".equals(mode)) {
                throw new IllegalArgumentException("windowMode must be foreground or background");
            }
            windowMode = mode;
        }
        return new OpenCliBrowserSession(executor, name, windowMode);
    }
}
