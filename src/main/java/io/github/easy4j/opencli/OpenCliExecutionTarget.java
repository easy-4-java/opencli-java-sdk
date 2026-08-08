package io.github.easy4j.opencli;

/**
 * Determines where an OpenCLI command is executed: as a local subprocess or via a remote
 Agent HTTP API compatible with opencli-admin.
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */public enum OpenCliExecutionTarget {

    /**
     * Launch a subprocess on the local machine using the {@code executable} configured in {@link OpenCliProperties} (default).
     */
    LOCAL_PROCESS,

    /**
     * 调用已部署的 OpenCLI Agent Server {@code POST /collect}（与
     * opencli-admin {@code backend/agent_server.py} 契约一致）。
     */
    REMOTE_AGENT_HTTP
}
