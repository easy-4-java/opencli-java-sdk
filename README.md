# opencli-java-sdk

面向 [OpenCLI](https://github.com/partme-ai/opencli) 多适配器体系的 Java SDK（JDK 17），工程风格对齐同仓库的 `dreamina-java-sdk`：Commons Exec 子进程封装、`OpenCliResult` / `OpenCliTypedResult`、统一异常语义。

## 功能概览

- **核心**：`OpenCliProperties`、`OpenCliExecutor`、`OpenCliAdapterChannel`（`opencli <adapter> ...`）
- **文档同步**：`OpenCliAdapterIds`、`OpenCliAdapterTaxonomy` 由 `scripts/generate_opencli_adapter_ids.py` 根据 `opencli/docs/adapters/index.md` 生成（共 134 个 adapter id）
- **分类门面**：`PublicApiClient`、`BrowserClient`、`DesktopClient`（亦可 `OpenCliClient#publicApis()` 等）
- **参考强类型封装**：`codex`、`cursor`、`gemini`、`claude`、`chatgpt`、`jimeng`、`arxiv`、`npm`、`binance`、`wikipedia`
- **批量遍历**：`OpenCliAdapterEnumerator` + `OpenCliAdapterIds.ALL`

## Maven

```xml
<dependency>
  <groupId>io.github.hiwepy</groupId>
  <artifactId>opencli-java-sdk</artifactId>
  <version>3.3.x.20260516-SNAPSHOT</version>
</dependency>
```

本地安装：

```bash
cd opencli-java-sdk && mvn clean install
```

## 快速开始

```java
OpenCliProperties props = new OpenCliProperties();
props.getEnvironment().put("OPENCLI_CDP_ENDPOINT", "http://127.0.0.1:9222");
OpenCliClient cli = new OpenCliClient(props);

// 任意 adapter（文档与 `opencli list` 一致）
OpenCliResult r = cli.adapter("hackernews").invoke("top", "--limit", "5");

// 强类型示例：npm
OpenCliTypedResult<com.fasterxml.jackson.databind.JsonNode> pkg =
    cli.npm().packageInfoTyped("react", null);

// Desktop：Codex
cli.codex().status();
cli.codex().ask("Summarize repo", null, null);
```

## 远程 Agent（opencli-admin）

与 [opencli-admin](https://github.com/partme-ai/opencli) 边缘 Agent（`backend/agent_server.py`，默认端口由 `AGENT_PORT` 决定，常见为 `19823`）对齐：走 `POST {base}/collect`，请求体为 adapter、command、`args` / `positional_args`、`format`、`mode`（`bridge` | `cdp`）等。

```java
OpenCliProperties props = new OpenCliProperties();
props.setExecutionTarget(io.github.hiwepy.opencli.OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
props.setRemoteAgentBaseUrl("http://agent-host:19823");
props.setRemoteCollectMode("cdp");       // 与 Agent 侧一致
props.setRemoteOutputFormat("json");
// props.setRemoteCdpEndpoint("http://127.0.0.1:9222"); // 可选，覆盖 Agent 默认 CDP
// props.setCommandTimeoutMillis(300_000);            // 复用为 HTTP 超时

OpenCliClient cli = new OpenCliClient(props);
OpenCliResult r = cli.adapter("npm").invoke("package", "react");
```

**调试**：若需要保留 Agent 返回的完整 HTTP 报文（与重组后的 `stdout` 对照），可设置 `remoteCaptureRawHttpResponse=true`（或配置键 `opencli.remote-capture-raw-http-response=true`），结果中 `OpenCliResult#getRemoteRawHttpBody()` 将有值；大响应时请谨慎开启。

**注意**

- 远程成功时，`stdout` 为 Agent 返回的 **`items` 的 JSON**（已按 `format` 解析成行列表），与本地子进程原始文本可能不同；强依赖整段 CLI stdout 的解析需单独评估。
- `leadingArguments` 等仅影响本地进程拼装，**远程路径不注入**。

## 中心 WebSocket（边缘反向 Agent）

opencli-admin 中 [`ws_agent_manager.py`](https://github.com/partme-ai/opencli/blob/main/opencli-admin/backend/ws_agent_manager.py) 描述中心经 WebSocket 向已注册边缘节点下发 `collect` 并等待 `result`。**中心侧的 `dispatch_collect` 仍由 Python FastAPI 持有**；本 SDK 提供的是**边缘 JVM 进程**与 Python `agent_server`（`AGENT_REGISTER=ws`）等价的客户端：连上中心 `ws(s)://…/api/v1/nodes/ws` 或 `…/api/v1/browsers/agents/ws`，发送 `register`，在收到 `collect` 时**本机**执行 `opencli`（通过 `OpenCliProperties#copyForLocalCliExecution()` 强制本地子进程，避免回路）。

协议摘要：

1. 边缘 → 中心：`{"type":"register","agent_url","mode","label","node_type"}`（与 `agent_server` 对齐）
2. 中心 → 边缘：`{"type":"registered","agent_url"}`
3. 中心 → 边缘：`{"type":"collect","request_id","site","command","args","positional_args","format","mode"}`
4. 边缘 → 中心：`{"type":"result","request_id","success","items","error"}`
5. `ping` / `pong`

```java
OpenCliProperties exec = new OpenCliProperties();
exec.getEnvironment().put("OPENCLI_CDP_ENDPOINT", "http://127.0.0.1:9222");

OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
conn.setCentralApiBaseUrl("http://center-host:8031");
conn.setAgentAdvertiseUrl("http://this-host:19823");
conn.setWebSocketPath(io.github.hiwepy.opencli.center.ws.OpenCliCenterWebSocketPath.NODES_WS);
conn.setMode("cdp");
conn.setNodeType("shell");
conn.setLabel("my-java-agent");

OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(exec, conn);
agent.start();
// …
agent.close();
```

类入口：`io.github.hiwepy.opencli.center.ws.OpenCliWsReverseAgentClient`。URL 拼接见 `OpenCliCenterWsUrls`。

## 刷新适配器常量

当上游 `index.md` 变更时：

```bash
python3 scripts/generate_opencli_adapter_ids.py
```

## 说明

- 未单独建模的子命令请使用 `OpenCliAdapterChannel#invoke(List)` 或可变参数透传。
- `-f json` 类结果可用 `OpenCliStdoutJson.typed(raw)` 转为 `JsonNode`（非 JSON 时降级为文本节点）。

## License

Apache License 2.0
