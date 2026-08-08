package io.github.easy4j.opencli.center.ws;

import io.github.easy4j.opencli.util.OpenCliStrings;

/**
 * Converts a central HTTP API base URL into the WebSocket URL used by edge Agents.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */public final class OpenCliCenterWsUrls {

    private OpenCliCenterWsUrls() {
    }

    /**
     * 例如 {@code http://192.168.1.1:8031} → {@code ws://192.168.1.1:8031/api/v1/nodes/ws}。
     *
     * @param centralHttpApiBase 中心 REST 根，如环境变量 {@code CENTRAL_API_URL}
     * @param path               反向通道路径
     * @return 完整 WS(S) URL
     */
    public static String toAgentWebSocketUrl(String centralHttpApiBase, OpenCliCenterWebSocketPath path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        String base = OpenCliStrings.isBlank(centralHttpApiBase) ? "" : centralHttpApiBase.trim();
        if (base.isEmpty()) {
            throw new IllegalArgumentException("centralHttpApiBase must not be blank");
        }
        base = base.replaceAll("/+$", "");
        String wsBase;
        if (base.startsWith("https://")) {
            wsBase = "wss://" + base.substring("https://".length());
        } else if (base.startsWith("http://")) {
            wsBase = "ws://" + base.substring("http://".length());
        } else {
            throw new IllegalArgumentException(
                "centralHttpApiBase must start with http:// or https://, got: " + base);
        }
        return wsBase + path.getPath();
    }
}
