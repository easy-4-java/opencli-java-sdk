package io.github.easy4j.opencli.center.ws;

import lombok.Data;

/**
 * 边缘 JVM 作为「反向 WebSocket Agent」连入 opencli-admin 中心所需的连接参数。
 * <p>
 * 行为对齐 {@code backend/agent_server.py} 中 {@code AGENT_REGISTER=ws}：向中心发送
 * {@code register}，随后在处理 {@code collect} 时本地执行 opencli，并以 {@code result} 回传。
 * </p>
 */
@Data
public class OpenCliWsAgentConnectionProperties {

    /**
     * 中心 HTTP API 根 URL（如 {@code http://host:8031}），与 Python {@code CENTRAL_API_URL} 一致。
     */
    private String centralApiBaseUrl;

    /**
     * 本节点对中心可见的 Agent HTTP 根（如 {@code http://edge-ip:19823}），即 {@code agent_url}。
     */
    private String agentAdvertiseUrl;

    /**
     * 连接中心的 WebSocket 路径枚举。
     */
    private OpenCliCenterWebSocketPath webSocketPath = OpenCliCenterWebSocketPath.NODES_WS;

    /**
     * 上报的 Chrome/opencli 模式：{@code bridge} | {@code cdp}。
     */
    private String mode = "cdp";

    /**
     * 展示用标签；可与主机名一致。
     */
    private String label = "";

    /**
     * 节点部署类型，仅在 {@link OpenCliCenterWebSocketPath#NODES_WS} 注册时由中心强校验：
     * {@code docker} | {@code shell}。
     */
    private String nodeType = "shell";

    /**
     * 首次 {@code registered} 应答等待上限（毫秒）。
     */
    private long handshakeTimeoutMillis = 15_000L;

    /**
     * 断线后最小重连间隔（毫秒）。
     */
    private long minReconnectDelayMillis = 3_000L;

    /**
     * 断线后最大重连间隔（毫秒）。
     */
    private long maxReconnectDelayMillis = 60_000L;
}
