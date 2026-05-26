# opencli-java-sdk

面向 [OpenCLI](https://github.com/partme-ai/opencli) 多适配器体系的 Java SDK，工程风格对齐同仓库的 `dreamina-java-sdk`：Commons Exec 子进程封装、`OpenCliResult` / `OpenCliTypedResult`、统一异常语义。远程 HTTP 使用 **Unirest**（与 `openclaw-java-sdk` 一致），中心 WebSocket 使用 **Java-WebSocket**，可在 JDK 8 线编译运行。

## 功能概览

- **核心**：`OpenCliProperties`、`OpenCliExecutor`、`OpenCliAdapterChannel`（`opencli <adapter> ...`）
- **文档同步**：`OpenCliAdapterIds`、`OpenCliAdapterTaxonomy` 由 `scripts/generate_opencli_adapter_ids.py` 根据 `opencli/docs/adapters/index.md` 生成（共 155 个 adapter id；可通过环境变量 `OPENCLI_ROOT` 指定上游 opencli 目录）
- **分类门面**：`PublicApiClient`、`BrowserClient`、`DesktopClient`（亦可 `OpenCliClient#publicApis()` 等）
- **参考强类型封装**：`codex`、`cursor`、`gemini`、`claude`、`chatgpt`、`jimeng`、`deepseek`、`arxiv`、`npm`、`pypi`、`binance`、`wikipedia`
- **元命令**：`OpenCliClient#meta()`（`list`、`validate`、`plugin`、`daemon`、`profile`、`completion` 等）
- **内置 browser**：`OpenCliClient#browser()` 会话 API（`wait` 的 `--timeout` 为毫秒；`extract` 使用 `--chunk-size`/`--start`）
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

## 多版本与 JDK（对齐 dreamina-java-sdk）

各 Git 分支通过脚本生成对应 `pom.xml`，**版本前缀、JDK、依赖栈随线变化**，勿混用（例如 2.7.x 线不能用 JDK 17 的坐标去跑 3.3.x 线）。

| 分支 | `artifact` 版本示例 | JDK | 说明 |
|------|----------------------|-----|------|
| `2.3.x` | `2.3.x.*-SNAPSHOT` | **8** | 与 Spring Boot 2.3.x 同线 |
| `2.7.x` | `2.7.x.*-SNAPSHOT` | **11** | 与 Spring Boot 2.7.x 同线 |
| `3.0.x` … `3.4.x` | `3.0.x.*` … `3.4.x.*` | **17** | 与 Spring Boot 3.0–3.4 一致 |
| `3.5.x` / `4.0.x` | `3.5.x.*` / `4.0.x.*` | **21** | 与 Spring Boot 3.5+ / 4.x 及 SLF4J 2.x 对齐 |

切分支后在本模块根目录执行：

```bash
python3 scripts/render-branch-pom.py <branch>   # 例: 3.3.x、2.7.x、4.0.x
```

发布到阿里云 Packages（各分支 `pom.xml` 已内置 `distributionManagement`）：

```bash
mvn clean deploy -Dmaven.test.skip=true
```

`settings.xml` 中配置私服 id：`2624322-release-6F6h6R`、`2624322-snapshot-3EoOv3`。

## 启动就绪探测（无 Spring）

```java
OpenCliAvailabilityChecker checker = new OpenCliAvailabilityChecker();
OpenCliAvailabilityReport report = checker.check(new OpenCliExecutor(props));
if (!report.isAvailable()) {
    throw new IllegalStateException(report.toDiagnosticMessage());
}
```

`execution-target=REMOTE_AGENT_HTTP` 时探测结果为 `SKIPPED_REMOTE_MODE`（视为可启动）。Spring Boot 见 [opencli-spring-boot-starter](../opencli-spring-boot-starter)。

## 快速开始

```java
OpenCliProperties props = new OpenCliProperties();
props.getEnvironment().put("OPENCLI_CDP_ENDPOINT", "http://127.0.0.1:9222");
OpenCliClient cli = new OpenCliClient(props);

// 任意 adapter（文档与 `opencli list` 一致）
OpenCliResult r = cli.adapter("hackernews").invoke("top", "--limit", "5");

// 强类型示例：Gemini deep-research（--confirm 为按钮文案，非布尔）
cli.gemini().deepResearch("topic",
    GeminiOpenCliClient.GeminiDeepResearchOptions.builder().confirmLabel("Start").build(), null);
```

### meta() 与 browser()

```java
cli.meta().list("json");
cli.meta().completion("zsh");
cli.browser().session("work", "background").waitFor("selector", ".loaded", null, 10_000L);
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

当上游 `index.md` 变更时（需可访问 opencli 源码树，默认相对路径或 `OPENCLI_ROOT`）：

```bash
export OPENCLI_ROOT=/path/to/opencli/opencli   # 可选
python3 scripts/generate_opencli_adapter_ids.py
```

## 说明

- 未单独建模的子命令请使用 `OpenCliAdapterChannel#invoke(List)` 或可变参数透传。
- `-f json` 类结果可用 `OpenCliStdoutJson.typed(raw)` 转为 `JsonNode`（非 JSON 时降级为文本节点）。

## License

Apache License 2.0
