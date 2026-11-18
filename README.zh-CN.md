# opencli-java-sdk

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-8-orange)](https://github.com/easy-4-java/opencli-java-sdk) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

面向 OpenCLI 多适配器体系的 Java SDK：browser / desktop / public-api 适配器、远程 Agent 与中心 WebSocket

> **维护线**：`feature/1.0.x`
> **版本**：`1.0.x.20260630-SNAPSHOT`
> **JDK 基线**：8
> **发布说明**：[1.0.x.20260630](https://github.com/easy-4-java/opencli-java-sdk/releases/tag/v1.0.x.20260630)

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 能力与状态](#2-features--status)
- [3. 运行要求与兼容性](#3-requirements--compatibility)
- [4. 架构与模块](#4-architecture--modules)
- [5. 引入依赖](#5-installation)
- [6. 快速开始](#6-quick-start)
- [7. 配置](#7-configuration)
- [8. 核心用法](#8-core-usage)
- [9. 测试与构建](#9-testing--build)
- [10. 版本线与分支](#10-versioning--branches)
- [11. 贡献与许可证](#11-contributing--license)

## 1. 项目概述

### 1.1 是什么

**opencli-java-sdk** 将 Java 应用与 [OpenCLI](https://github.com/partme-ai/opencli) 多适配器 CLI 生态对接。它通过 Commons Exec 执行 `opencli <adapter> ...` 子进程，提供类型化结果（`OpenCliResult` / `OpenCliTypedResult`）与统一异常语义，并支持远程 HTTP Agent（Apache HttpClient）与中心 WebSocket 反向 Agent 客户端（Java-WebSocket）。可在 JDK 8 线编译运行。

### 1.2 不是什么

- 不是 OpenCLI 本身，也不是浏览器自动化引擎——它驱动 `opencli` CLI。
- 无 Spring 依赖；Spring Boot 应用请使用配套的 `opencli-spring-boot-starter`。
- 不为新适配器生成 SDK；适配器 ID 由上游 `opencli/docs/adapters/index.md` 清单生成。

### 1.3 典型使用场景

| 场景 | 推荐入口 | 结果 |
|---|---|---|
| 执行任意适配器命令 | `cli.adapter("hackernews").invoke("top", "--limit", "5")` | 类型化 `OpenCliResult` |
| 已知适配器的强类型封装 | `cli.gemini().deepResearch(...)`、`cli.npm()`、`cli.codex()` ... | 强类型参数与结果 |
| 批量遍历全部适配器 | `OpenCliAdapterEnumerator` + `OpenCliAdapterIds.ALL` | 顺序执行适配器 |
| 通过远程 Agent 执行 | `executionTarget=REMOTE_AGENT_HTTP` + `remoteAgentBaseUrl` | `POST {base}/collect` 执行 |
| 以边缘节点加入中心 | `OpenCliWsReverseAgentClient` | 注册、接收 `collect`、回执 `result` |

<a id="2-features--status"></a>
## 2. 能力与状态

| 能力 | 状态 | 说明 |
|---|:---:|---|
| 本地子进程执行 | 可用 | `OpenCliExecutor`（Commons Exec），统一异常（`OpenCliNonZeroExitException`、`OpenCliTimeoutException` 等） |
| 适配器通道 | 可用 | `OpenCliAdapterChannel`（`invoke(List)` / 可变参数） |
| 适配器注册表 | 可用 | `OpenCliAdapterIds` + `OpenCliAdapterTaxonomy`——共 173 个 adapter id（163 browser + 10 desktop），由上游清单生成 |
| 强类型封装 | 可用 | `codex`、`cursor`、`gemini`、`claude`、`chatgpt`、`jimeng`、`deepseek`、`kimi`、`qwen`、`doubao`、`grok`、`yuanbao`、`arxiv`、`npm`、`pypi`、`binance`、`wikipedia` |
| 分类门面 | 可用 | `PublicApiClient`、`BrowserClient`、`DesktopClient`（或 `publicApis()` / `browsers()` / `desktops()`） |
| 元命令 | 可用 | `cli.meta()`：`list`、`validate`、`plugin`、`daemon`、`profile`、`completion`、`skills`、`auth`、`antigravity` 等 |
| 内置 browser 会话 API | 可用 | `cli.browser()`：`wait`（毫秒超时）、`extract`、`screenshot`、`getHtml` 等 |
| 启动就绪探测 | 可用 | `OpenCliAvailabilityChecker` + `OpenCliAvailabilityReport`（远程模式报告 `SKIPPED_REMOTE_MODE`） |
| 远程 Agent（HTTP） | 可用 | `OpenCliRemoteAgentHttpClient`；`remoteCaptureRawHttpResponse` 调试开关 |
| 中心 WebSocket 反向 Agent | 可用 | `OpenCliWsReverseAgentClient`（register / collect / result / ping-pong） |
| JSON 输出解析 | 可用 | `OpenCliStdoutJson.typed(raw)`；`OpenCliParsedFields` |

<a id="3-requirements--compatibility"></a>
## 3. 运行要求与兼容性

| 组件 | 版本 | 说明 |
|---|---:|---|
| JDK | 8+ | 1.0.x 线基线 |
| Maven | 3.9.16 | 仓库 Maven Wrapper 版本 |
| commons-exec | 1.6.0 | 本地子进程执行 |
| Apache HttpClient | 由 Unirest Java 3.14.5 提供 | 远程 Agent HTTP |
| Java-WebSocket | 1.6.0 | 中心 WebSocket |
| `com.fasterxml.jackson.core:jackson-databind` | 2.18.9 | JSON 解析 |
| SLF4J | 2.0.18 | 日志门面 |

版本线矩阵：

| 版本线 | 分支 | JDK | 版本模式 | 用途 |
|---|---|---:|---|---|
| 1.0.x | `feature/1.0.x` | 8 | `1.0.x.*` | 供 Boot 2.x Starter 与存量项目使用 |
| 2.0.x | `feature/2.0.x` | 17 | `2.0.x.*` | 供 Boot 3.x Starter 使用 |
| 3.0.x | `feature/3.0.x` | 21 | `3.0.x.*` | 供 Boot 4.x Starter / 新项目使用 |

<a id="4-architecture--modules"></a>
## 4. 架构与模块

```text
[ Java 应用 ]
        |
        | opencli-java-sdk
        v
+------------------------------------------+
| OpenCliClient（门面）                     |
|  core      OpenCliExecutor -> 本地        |
|            `opencli <adapter> ...`        |
|  adapters  browser / desktop / publicapi  |
|            强类型封装（codex、npm...）     |
|  meta      list / validate / plugin / ... |
|  browser   会话 API（wait、extract...）    |
|  remote    POST {base}/collect（Apache HttpClient） |
|  center    WebSocket 反向 Agent           |
+------------------------------------------+
        |
        v
[ opencli CLI ] / [ 远程 Agent ] / [ 中心 WS ]
```

单模块库（打包类型 `jar`）。包结构：

| 包 | 职责 |
|---|---|
| `io.github.easy4j.opencli` | 门面 `OpenCliClient`、`OpenCliProperties`、`OpenCliExecutionTarget` |
| `io.github.easy4j.opencli.core` | `OpenCliExecutor`、`OpenCliAdapterChannel`、结果对象、可用性探测 |
| `io.github.easy4j.opencli.adapter` | 强类型适配器客户端（browser：chatgpt/claude/deepseek/gemini/jimeng/kimi/qwen/doubao/grok/yuanbao；desktop：codex/cursor；publicapi：arxiv/binance/npm/pypi/wikipedia） |
| `io.github.easy4j.opencli.browser` | 内置 browser 会话客户端与选项 |
| `io.github.easy4j.opencli.facade` | `PublicApiClient` / `BrowserClient` / `DesktopClient` |
| `io.github.easy4j.opencli.meta` | 元命令客户端（`list`、`plugin`、`daemon`、`profile`、`skills`、`auth` 等） |
| `io.github.easy4j.opencli.registry` | `OpenCliAdapterIds`（173 个 id）、`OpenCliAdapterTaxonomy` |
| `io.github.easy4j.opencli.remote` | 远程 Agent HTTP 客户端 |
| `io.github.easy4j.opencli.center.ws` | 中心 WebSocket 反向 Agent 与路径 / URL 工具 |
| `io.github.easy4j.opencli.parser` / `spi` / `util` | Stdout-JSON 解析、适配器枚举 SPI、工具类 |

<a id="5-installation"></a>
## 5. 引入依赖

Maven：

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>opencli-java-sdk</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

Gradle：

```groovy
implementation 'io.github.easy4j:opencli-java-sdk:1.0.x.20260630-SNAPSHOT'
```

快照版本需要启用对应快照仓库（`pom.xml` 中 `distributionManagement` 指向 Aliyun Maven 仓库）。

<a id="6-quick-start"></a>
## 6. 快速开始

```java
OpenCliProperties props = new OpenCliProperties();
props.getEnvironment().put("OPENCLI_CDP_ENDPOINT", "http://127.0.0.1:9222");

OpenCliClient cli = new OpenCliClient(props);

// 任意适配器（id 与 `opencli list` / 生成注册表一致）
OpenCliResult r = cli.adapter("hackernews").invoke("top", "--limit", "5");
System.out.println(r.getExitCode() + ": " + r.getStdout());

// 强类型示例：Gemini deep-research（--confirm 为按钮文案，非布尔）
cli.gemini().deepResearch("topic",
        GeminiOpenCliClient.GeminiDeepResearchOptions.builder().confirmLabel("Start").build(),
        null);
```

**预期结果**：第一个调用在本地启动 `opencli hackernews top --limit 5` 并返回类型化 `OpenCliResult`；第二个调用以类型化参数执行 Gemini 适配器的 deep-research 命令。当 `opencli` 可执行文件缺失时，`OpenCliExecutor` 通过统一异常层级（`OpenCliStartupException` / `OpenCliExecutableFailureException`）暴露失败。

<a id="7-configuration"></a>
## 7. 配置

配置为对象式，通过 `OpenCliProperties`：

| 属性 | 默认值 | 说明 |
|---|---|---|
| `executionTarget` | `LOCAL_PROCESS` | `LOCAL_PROCESS` 或 `REMOTE_AGENT_HTTP` |
| `executable` | `opencli` | 可执行文件名或绝对路径 |
| `workingDirectory` | — | 子进程工作目录 |
| `commandTimeoutMillis` | `300000` | 命令超时（毫秒）。远程连接池等待、建连和读取响应共享调用预算。 |
| `maxConcurrentExecutions` | `0` | 最大并发子进程数（0 = 不限） |
| `startupProbeTimeoutMillis` | `30000` | 可用性探测超时 |
| `environment` | `{}` | 额外环境变量（如 `OPENCLI_CDP_ENDPOINT`） |
| `remoteAgentBaseUrl` | — | 远程 Agent 的 HTTP(S) 基址，可以保留部署路径前缀。省略用户信息、查询参数和片段。 |
| `remoteCollectMode` | `cdp` | `bridge` 或 `cdp`（须与 Agent 侧一致） |
| `remoteOutputFormat` | `json` | 向 Agent 请求的输出格式 |
| `remoteCdpEndpoint` | — | 可选，覆盖 Agent 默认 CDP 端点 |
| `remoteCaptureRawHttpResponse` | `false` | 在 `OpenCliResult#getRemoteRawHttpBody()` 中保留原始 HTTP 报文（大响应请谨慎开启） |
| `maxRemoteResponseBytes` | `10485760` | HTTP 解压后的响应体字节上限，须为正数。超限抛出 `OpenCliRemoteResponseLimitException`，含 HTTP 状态和上限。原文捕获受同一上限约束。 |

注意事项：

- 远程模式下 `stdout` 为 Agent 返回的 `items` 的 JSON（已按 `format` 解析为行列表），与本地原始子进程文本可能不同；`leadingArguments` 等仅影响本地进程拼装，**远程路径不注入**。
- 旧版 HTTP `/collect` 响应须包含 JSON 布尔字段 `success`。无效响应抛出 `OpenCliRemoteProtocolException`，它继承 `OpenCliExecutableFailureException`。当 `success: true` 且 `items` 缺失或为 null 时，仍返回 `[]`。
- 旧版 HTTP argv 调用仅接收 SDK 清单中的适配器及其子命令。根命令、Browser 会话、外部程序透传、重复选项、`--` 以及无法保留边界空格或前导短横的参数会在发请求前被拒绝。位置参数应在选项前。需要原样传递 argv 时使用本地执行，已有自定义 Agent 契约时可用显式 `OpenCliCollectRequest` API。
- 远程调用超时抛出 `OpenCliRemoteTimeoutException`。请求提交给 HTTP 传输后，`isRemoteOutcomeUnknown()` 为 true。客户端不会自动重试或跟随重定向。
- 通过结果或异常的 `getPartialResult()` 调用 `getRemoteExecutionDetails()`，查看 HTTP 状态、Agent 业务状态和 `items` 的来源。`getExitCode()` 的 0/1 是兼容映射。HTTP 或协议失败快照没有退出码。仅在开启原文捕获时，失败快照才携带已通过字节上限检查的响应正文。
- Agent 支持认证请求头时，可调用 `setRemoteAuthentication(new OpenCliRemoteAuthentication("Authorization", System.getenv("OPENCLI_AGENT_AUTHORIZATION")))`，传入完整请求头值。认证须使用 HTTPS，Agent 基础地址须省略用户信息、查询参数和片段。SDK 只在 `/collect` 请求上发送已配置的认证头。OAuth 发现和请求签名尚不支持。
- `execution-target=REMOTE_AGENT_HTTP` 时可用性探测报告 `SKIPPED_REMOTE_MODE`（视为可启动）。

<a id="8-core-usage"></a>
## 8. 核心用法

### 8.1 meta() 与 browser()

```java
cli.meta().list("json");
cli.meta().completion("zsh");
cli.browser().session("work", "background").waitFor("selector", ".loaded", null, 10_000L);
```

### 8.2 中心 WebSocket 反向 Agent

```java
OpenCliProperties exec = new OpenCliProperties();
exec.getEnvironment().put("OPENCLI_CDP_ENDPOINT", "http://127.0.0.1:9222");

OpenCliWsAgentConnectionProperties conn = new OpenCliWsAgentConnectionProperties();
conn.setCentralApiBaseUrl("http://center-host:8031");
conn.setAgentAdvertiseUrl("http://this-host:19823");
conn.setWebSocketPath(io.github.easy4j.opencli.center.ws.OpenCliCenterWebSocketPath.NODES_WS);
conn.setMode("cdp");
conn.setNodeType("shell");
conn.setLabel("my-java-agent");

OpenCliWsReverseAgentClient agent = new OpenCliWsReverseAgentClient(exec, conn);
agent.start();
// ...
agent.close();
```

协议摘要：边缘发送 `register`；中心回执 `registered`；中心下发 `collect`（request_id、site、command、args、positional_args、format、mode）；SDK 在本机执行 `opencli`（通过 `copyForLocalCliExecution()` 强制本地子进程，避免回路）并回执 `result`；另有 `ping` / `pong`。

反向 Agent 默认最多同时执行 4 个 collect 任务，另有 32 个等待名额。构建 Agent 前，可在连接配置中设置 `maxConcurrentCollects` 和 `maxPendingCollects`。等待容量为零时直接拒绝超额请求。容量用尽时返回失败的 `result`，错误为 `worker is busy`。

当前连接收到合法注册确认后，反向 Agent 才开始收单。入站 WebSocket 消息默认上限为 1 MiB，分片按聚合后的字节数计量。需要调整上限时，在创建 Agent 前设置有限的 `maxInboundMessageBytes`。超限消息会关闭对应连接，不会提交命令。

collect 请求 ID 必须为非空白字符串，长度上限 128 个 UTF-8 字节。相同的在途请求只执行一次，同一个 ID 对应不同载荷时会明确拒绝。显式重试已完成的请求有可能收到标记为 `cached: true` 的结果。内存缓存最多保留 1,024 条完成结果，每条最长 10 分钟。跨连接重放要求使用同一个经过 TLS 校验的端点，明文 WebSocket 的去重范围为当前连接。创建新 Agent 实例时从空缓存开始。

`stop()` 停止接单并丢弃等待任务，为已在途任务保留最多 5 秒的收尾时间，随后请求取消。连接线程退出后，同一实例可再次调用 `start()` 重连。`close()` 立即请求取消，并永久关闭该实例。

### 诊断导出

如需导出诊断，请调用 `OpenCliDiagnosticExport.export(result, 512)`，传入 `OpenCliResult` 或异常中的部分结果。上限按 UTF-8 字节计算。输出只包含状态和字段长度，字段放不下时会标明 `truncated=true`。字段长度仍可能反映数据规模。

SDK 在内存中返回文本，由应用决定目的地和保留时间。SDK 诊断会排除命令参数、原始内容、URL 和文件路径。应用读取或记录原始结果时应自行保护这些数据。`remoteCaptureRawHttpResponse` 仍需显式开启。

低层进程接口的 `RunSession.getHandler().getException()` 返回安全的 I/O 摘要。`RunSession.getIoFailure()` 是显式读取原始诊断的入口，异常可能包含本地路径，应按敏感数据处理。

<a id="9-testing--build"></a>
## 9. 测试与构建

```bash
mvn clean verify
```

- 单元测试覆盖适配器注册表、核心执行、browser / meta / remote 路径与 WS 工具（`src/test` 下 19 个 Java 测试源文件）。
- JaCoCo 在 `verify` 阶段执行 `prepare-agent`、`report` 与 `check`，行覆盖率规则为 **90%**（`haltOnFailure=false`）。
- `scripts/generate_opencli_adapter_ids.py` 依据上游 `opencli/docs/adapters/index.md` + `cli-manifest.json` 重新生成 `OpenCliAdapterIds` / `OpenCliAdapterTaxonomy`（可用 `OPENCLI_ROOT` 指定上游源码树）。
- 发布打包（`mvn -Prelease deploy`）附带 sources 与 javadoc 构件并执行 GPG 签名，对接 Sonatype Central Publishing；普通 `mvn deploy` 按版本后缀路由到 Aliyun Maven 仓库（见 `distributionManagement`）。

<a id="10-versioning--branches"></a>
## 10. 版本线与分支

| 分支 | 版本模式 | JDK | 维护策略 |
|---|---|---|---|
| `feature/1.0.x` | `1.0.x.*` | 8 | 仅接受兼容性修复与 JDK 8 安全的依赖升级 |
| `feature/2.0.x` | `2.0.x.*` | 17 | JDK 17 线 |
| `feature/3.0.x` | `3.0.x.*` | 21 | JDK 21 线 |

各分支 POM（JDK 与依赖栈随线变化）由 `scripts/render-branch-pom.py` 渲染。

<a id="11-contributing--license"></a>
## 11. 贡献与许可证

欢迎贡献。提交 Pull Request 前请执行 `mvn clean verify`，并说明兼容性、测试与迁移影响。本项目采用 [Apache License 2.0](LICENSE) 许可证。
