package io.github.easy4j.opencli.meta;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli antigravity serve} — Anthropic 兼容 API 代理。
 * <p>
 * <strong>注意：</strong>{@link #serve(Integer, Integer)} 会启动长驻 HTTP 服务并阻塞当前线程直至进程退出，
 * 不适合在请求线程中直接调用；建议在独立进程或 {@code ExecutorService} 中运行。
 * </p>
 */
@RequiredArgsConstructor/**

 * Client for {@code opencli antigravity serve} — an Anthropic-compatible API proxy.
 *
 * <p><b>Note:</b> {@link #serve(Integer, Integer)} starts a long-running HTTP server
 * and blocks the current thread until the process exits. Do not call directly from a
 * request thread; run in a separate process or {@code ExecutorService}.</p>

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public final class OpenCliAntigravityClient {

    private final OpenCliExecutor executor;

    /**
     * 启动 {@code antigravity serve}（阻塞）。
     *
     * @param port           监听端口，null 使用 CLI 默认 8082
     * @param timeoutSeconds 回复等待秒数，null 使用 CLI 默认
     */
    public OpenCliResult serve(Integer port, Integer timeoutSeconds) {
        List<String> args = new ArrayList<>();
        args.add("antigravity");
        args.add("serve");
        if (port != null) {
            OpenCliArgSupport.addOptionPair(args, "--port", String.valueOf(port));
        }
        if (timeoutSeconds != null) {
            OpenCliArgSupport.addOptionPair(args, "--timeout", String.valueOf(timeoutSeconds));
        }
        return executor.invoke(args);
    }
}
