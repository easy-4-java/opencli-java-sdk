# opencli-java-sdk

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-8-orange)](https://github.com/easy-4-java/opencli-java-sdk) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

面向 OpenCLI 多适配器体系的 Java SDK：browser / desktop / public-api 适配器、远程 Agent 与中心 WebSocket

> **当前分支**：`feature/1.0.x`
> **版本**：`1.0.x.20260630-SNAPSHOT`
> **JDK 基线**：8
> **项目状态**：稳定（1.0.x 线）。尚未发布 Maven Central；制品通过 Aliyun Maven 仓库与 GitHub Releases 分发。

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

**opencli-java-sdk** 将 Java 应用与 [OpenCLI](https://github.com/partme-ai/opencli) 多适配器 CLI 生态对接。它通过 Commons Exec 执行 `opencli <adapter> ...` 子进程，提供类型化结果（`OpenCliResult` / `OpenCliTypedResult`）与统一异常语义，并支持远程 HTTP Agent（Unirest）与中心 WebSocket 反向 Agent 客户端（Java-WebSocket）。可在 JDK 8 线编译运行。

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
| 强类型封装 | 可用 | `codex`、`cursor`、`gemini`、`claude`、`chatgpt`、`jimeng`、`deepseek`、`arxiv`、`npm`、`pypi`、`binance`、`wikipedia` |
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
| commons-exec | — | 本地子进程执行 |
| Unirest Java | — | 远程 Agent HTTP |
| Java-WebSocket | — | 中心 WebSocket |
| Jackson databind | 2.17.x | JSON 解析 |
| SLF4J | 2.0.18 | 日志门面 |

版本线矩阵：

| 版本线 | 分支 | JDK | 版本模式 | 用途 |
|---|---|---:|---|---|
| 1.0.x | `feature/1.0.x`（当前分支） | 8 | `1.0.x.*` | 供 Boot 2.x Starter 与存量项目使用 |
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
|  remote    POST {base}/collect（Unirest） |
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
| `io.github.easy4j.opencli.adapter` | 强类型适配器客户端（browser：chatgpt/claude/deepseek/gemini/jimeng；desktop：codex/cursor；publicapi：arxiv/binance/npm/pypi/wikipedia） |
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
| `commandTimeoutMillis` | `300000` | 命令超时（毫秒；远程模式复用为 HTTP 超时） |
| `maxConcurrentExecutions` | `0` | 最大并发子进程数（0 = 不限） |
| `startupProbeTimeoutMillis` | `30000` | 可用性探测超时 |
| `environment` | `{}` | 额外环境变量（如 `OPENCLI_CDP_ENDPOINT`） |
| `remoteAgentBaseUrl` | — | 远程 Agent 根地址 |
| `remoteCollectMode` | `cdp` | `bridge` 或 `cdp`（须与 Agent 侧一致） |
| `remoteOutputFormat` | `json` | 向 Agent 请求的输出格式 |
| `remoteCdpEndpoint` | — | 可选，覆盖 Agent 默认 CDP 端点 |
| `remoteCaptureRawHttpResponse` | `false` | 在 `OpenCliResult#getRemoteRawHttpBody()` 中保留原始 HTTP 报文（大响应请谨慎开启） |

注意事项：

- 远程模式下 `stdout` 为 Agent 返回的 `items` 的 JSON（已按 `format` 解析为行列表），与本地原始子进程文本可能不同；`leadingArguments` 等仅影响本地进程拼装，**远程路径不注入**。
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

<a id="9-testing--build"></a>
## 9. 测试与构建

```bash
mvn clean verify
```

- 单元测试覆盖适配器注册表、核心执行、browser / meta / remote 路径与 WS 工具（`src/test` 下 14 个测试源文件）。
- JaCoCo 在 `verify` 阶段执行 `prepare-agent`、`report` 与 `check`，行覆盖率规则为 **90%**（`haltOnFailure=false`）。
- `scripts/generate_opencli_adapter_ids.py` 依据上游 `opencli/docs/adapters/index.md` + `cli-manifest.json` 重新生成 `OpenCliAdapterIds` / `OpenCliAdapterTaxonomy`（可用 `OPENCLI_ROOT` 指定上游源码树）。
- 发布打包（`mvn -Prelease deploy`）附带 sources 与 javadoc 构件并执行 GPG 签名，对接 Sonatype Central Publishing；普通 `mvn deploy` 按版本后缀路由到 Aliyun Maven 仓库（见 `distributionManagement`）。

<a id="10-versioning--branches"></a>
## 10. 版本线与分支

| 分支 | 版本模式 | JDK | 维护策略 |
|---|---|---|---|
| `feature/1.0.x`（当前分支） | `1.0.x.*` | 8 | 仅接受兼容性修复与 JDK 8 安全的依赖升级 |
| `feature/2.0.x` | `2.0.x.*` | 17 | JDK 17 线 |
| `feature/3.0.x` | `3.0.x.*` | 21 | JDK 21 线 |

各分支 POM（JDK 与依赖栈随线变化）由 `scripts/render-branch-pom.py` 渲染。

<a id="11-contributing--license"></a>
## 11. 贡献与许可证

欢迎贡献。提交 Pull Request 前请执行 `mvn clean verify`，并说明兼容性、测试与迁移影响。本项目采用 [Apache License 2.0](LICENSE) 许可证。
