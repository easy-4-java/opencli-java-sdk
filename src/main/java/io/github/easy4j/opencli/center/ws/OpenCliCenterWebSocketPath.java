package io.github.easy4j.opencli.center.ws;

/**
 * Reverse WebSocket paths exposed by the opencli-admin central server
 * (edge Agents connect as clients).
 *
 * <p>Endpoints are consistent with {@code backend/api/v1/nodes.py} and
 * {@code backend/api/v1/browsers.py} registration endpoints.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */public enum OpenCliCenterWebSocketPath {

    /**
     * {@code agent_server} 默认：{@code /api/v1/nodes/ws}；注册报文需合法 {@code node_type}（{@code docker} | {@code shell}）。
     */
    NODES_WS("/api/v1/nodes/ws"),

    /**
     * 浏览器池路由：{@code /api/v1/browsers/agents/ws}（文档见 {@code ws_agent_manager} 头部注释）。
     */
    BROWSERS_AGENTS_WS("/api/v1/browsers/agents/ws");

    private final String path;

    OpenCliCenterWebSocketPath(String path) {
        this.path = path;
    }

    /**
     * @return 以 {@code /} 开头的路径段，需拼在中心 HTTP(S) 根 URL 对应的 WS(S) 基址之后
     */
    public String getPath() {
        return path;
    }
}
