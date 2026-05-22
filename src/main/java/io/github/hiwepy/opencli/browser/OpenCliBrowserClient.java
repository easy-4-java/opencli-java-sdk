package io.github.hiwepy.opencli.browser;

import io.github.hiwepy.opencli.core.OpenCliExecutor;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI 内置 {@code browser} 命令门面：会话优先 API {@code browser(session).open(url)}。
 */
@RequiredArgsConstructor
public final class OpenCliBrowserClient {

    private final OpenCliExecutor executor;

    /**
     * 绑定具名浏览器会话；同一 {@code sessionName} 可跨调用保持 tab/状态。
     *
     * @param sessionName 会话名（对应 CLI 第一个 positional）
     * @return 会话作用域客户端
     */
    public OpenCliBrowserSession session(String sessionName) {
        String name = Objects.requireNonNull(sessionName, "sessionName").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("sessionName must not be blank");
        }
        return new OpenCliBrowserSession(executor, name);
    }
}
