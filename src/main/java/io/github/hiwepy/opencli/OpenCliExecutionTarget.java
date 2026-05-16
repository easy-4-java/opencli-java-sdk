package io.github.hiwepy.opencli;

/**
 * OpenCLI 命令实际执行位置：本机子进程或与 opencli-admin 兼容的远端 Agent HTTP API。
 */
public enum OpenCliExecutionTarget {

    /**
     * 在本机通过 {@link OpenCliProperties#getExecutable()} 启动子进程（默认）。
     */
    LOCAL_PROCESS,

    /**
     * 调用已部署的 OpenCLI Agent Server {@code POST /collect}（与
     * opencli-admin {@code backend/agent_server.py} 契约一致）。
     */
    REMOTE_AGENT_HTTP
}
