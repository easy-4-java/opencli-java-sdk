# opencli-java-sdk

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-21-orange)](https://github.com/easy-4-java/opencli-java-sdk) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

Multi-adapter CLI integration SDK for OpenCLI: browser / desktop / public-API adapters, remote agent and center WebSocket
[简体中文](./README.zh-CN.md)

> **Current branch**: `feature/3.0.x`
> **Version**: `3.0.x.20260630-SNAPSHOT`
> **JDK baseline**: 21
> **Project status**: stable (3.0.x line). Not yet published to Maven Central; artifacts are distributed via the Aliyun Maven repository and GitHub Releases.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage](#8-core-usage)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

### 1.1 What it is

**opencli-java-sdk** integrates Java applications with the [OpenCLI](https://github.com/partme-ai/opencli) multi-adapter CLI ecosystem. It executes `opencli <adapter> ...` subprocesses via Commons Exec, exposes typed results (`OpenCliResult` / `OpenCliTypedResult`), unified exception semantics, remote HTTP agent support (Unirest) and a center WebSocket reverse-agent client (Java-WebSocket). It compiles and runs on JDK 8.

### 1.2 What it is not

- Not OpenCLI itself and not a browser automation engine — it drives the `opencli` CLI.
- No Spring dependency; Spring Boot applications use the companion `opencli-spring-boot-starter`.
- Not an SDK generator for new adapters; adapter IDs are generated from the upstream `opencli/docs/adapters/index.md` manifest.

### 1.3 Typical scenarios

| Scenario | Recommended entry | Result |
|---|---|---|
| Run any adapter command | `cli.adapter("hackernews").invoke("top", "--limit", "5")` | Typed `OpenCliResult` |
| Typed wrapper for a known adapter | `cli.gemini().deepResearch(...)`, `cli.npm()`, `cli.codex()` ... | Strongly-typed options and results |
| Batch over all adapters | `OpenCliAdapterEnumerator` + `OpenCliAdapterIds.ALL` | Sequential adapter execution |
| Run commands through a remote agent | `executionTarget=REMOTE_AGENT_HTTP` + `remoteAgentBaseUrl` | `POST {base}/collect` execution |
| Join a center as an edge node | `OpenCliWsReverseAgentClient` | Register, receive `collect`, reply `result` |

<a id="2-features--status"></a>
## 2. Features & Status

| Capability | Status | Notes |
|---|:---:|---|
| Local subprocess execution | Available | `OpenCliExecutor` (Commons Exec), unified exceptions (`OpenCliNonZeroExitException`, `OpenCliTimeoutException`, ...) |
| Adapter channel | Available | `OpenCliAdapterChannel` (`invoke(List)` / varargs) |
| Adapter registry | Available | `OpenCliAdapterIds` + `OpenCliAdapterTaxonomy` — 173 adapter ids (163 browser + 10 desktop) generated from the upstream manifest |
| Typed wrappers | Available | `codex`, `cursor`, `gemini`, `claude`, `chatgpt`, `jimeng`, `deepseek`, `arxiv`, `npm`, `pypi`, `binance`, `wikipedia` |
| Categorized facades | Available | `PublicApiClient`, `BrowserClient`, `DesktopClient` (or `publicApis()` / `browsers()` / `desktops()`) |
| Meta commands | Available | `cli.meta()`: `list`, `validate`, `plugin`, `daemon`, `profile`, `completion`, `skills`, `auth`, `antigravity`, ... |
| Built-in browser session API | Available | `cli.browser()`: `wait` (ms timeout), `extract`, `screenshot`, `getHtml`, ... |
| Availability probe | Available | `OpenCliAvailabilityChecker` + `OpenCliAvailabilityReport` (remote mode reports `SKIPPED_REMOTE_MODE`) |
| Remote agent (HTTP) | Available | `OpenCliRemoteAgentHttpClient`; `remoteCaptureRawHttpResponse` debug flag |
| Center WebSocket reverse agent | Available | `OpenCliWsReverseAgentClient` (register / collect / result / ping-pong) |
| JSON output parsing | Available | `OpenCliStdoutJson.typed(raw)`; `OpenCliParsedFields` |

<a id="3-requirements--compatibility"></a>
## 3. Requirements & Compatibility

| Component | Version | Notes |
|---|---:|---|
| JDK | 21+ | 1.0.x line baseline |
| commons-exec | — | Local subprocess execution |
| Unirest Java | — | Remote agent HTTP |
| Java-WebSocket | — | Center WebSocket |
| Jackson databind | 2.17.x | JSON parsing |
| SLF4J | 2.0.18 | Logging facade |

Version-line matrix:

| Version line | Branch | JDK | Version pattern | Purpose |
|---|---|---:|---|---|
| 1.0.x | `feature/1.0.x` | 8 | `1.0.x.*` | For Boot 2.x starters and legacy projects |
| 2.0.x | `feature/2.0.x` | 17 | `2.0.x.*` | For Boot 3.x starters |
| 3.0.x | `feature/3.0.x` | 21 | `3.0.x.*` | For Boot 4.x starters / new projects |

<a id="4-architecture--modules"></a>
## 4. Architecture & Modules

```text
[ Java Application ]
        |
        | opencli-java-sdk
        v
+------------------------------------------+
| OpenCliClient (facade)                    |
|  core      OpenCliExecutor -> local       |
|            `opencli <adapter> ...`        |
|  adapters  browser / desktop / publicapi  |
|            typed wrappers (codex, npm...) |
|  meta      list / validate / plugin / ... |
|  browser   session API (wait, extract...) |
|  remote    POST {base}/collect (Unirest)  |
|  center    WebSocket reverse agent        |
+------------------------------------------+
        |
        v
[ opencli CLI ] / [ remote agent ] / [ center WS ]
```

Single-module library (packaging `jar`). Package layout:

| Package | Responsibility |
|---|---|
| `io.github.easy4j.opencli` | Facade `OpenCliClient`, `OpenCliProperties`, `OpenCliExecutionTarget` |
| `io.github.easy4j.opencli.core` | `OpenCliExecutor`, `OpenCliAdapterChannel`, results, availability |
| `io.github.easy4j.opencli.adapter` | Typed adapter clients (browser: chatgpt/claude/deepseek/gemini/jimeng; desktop: codex/cursor; publicapi: arxiv/binance/npm/pypi/wikipedia) |
| `io.github.easy4j.opencli.browser` | Built-in browser session client + options |
| `io.github.easy4j.opencli.facade` | `PublicApiClient` / `BrowserClient` / `DesktopClient` |
| `io.github.easy4j.opencli.meta` | Meta clients (`list`, `plugin`, `daemon`, `profile`, `skills`, `auth`, ...) |
| `io.github.easy4j.opencli.registry` | `OpenCliAdapterIds` (173 ids), `OpenCliAdapterTaxonomy` |
| `io.github.easy4j.opencli.remote` | Remote agent HTTP client |
| `io.github.easy4j.opencli.center.ws` | Center WebSocket reverse agent + path/URL helpers |
| `io.github.easy4j.opencli.parser` / `spi` / `util` | Stdout-JSON parsing, adapter enumeration SPI, helpers |

<a id="5-installation"></a>
## 5. Installation

Maven:

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>opencli-java-sdk</artifactId>
    <version>3.0.x.20260630-SNAPSHOT</version>
</dependency>
```

Gradle:

```groovy
implementation 'io.github.easy4j:opencli-java-sdk:3.0.x.x.20260630-SNAPSHOT'
```

Snapshot builds require an enabled snapshot repository (Aliyun Maven snapshot repository per `distributionManagement` in `pom.xml`).

<a id="6-quick-start"></a>
## 6. Quick Start

```java
OpenCliProperties props = new OpenCliProperties();
props.getEnvironment().put("OPENCLI_CDP_ENDPOINT", "http://127.0.0.1:9222");

OpenCliClient cli = new OpenCliClient(props);

// Any adapter (ids match `opencli list` / the generated registry)
OpenCliResult r = cli.adapter("hackernews").invoke("top", "--limit", "5");
System.out.println(r.getExitCode() + ": " + r.getStdout());

// Typed example: Gemini deep-research (--confirm is the button label, not a boolean)
cli.gemini().deepResearch("topic",
        GeminiOpenCliClient.GeminiDeepResearchOptions.builder().confirmLabel("Start").build(),
        null);
```

**Expected result**: the first call spawns `opencli hackernews top --limit 5` locally and returns a typed `OpenCliResult`; the second invokes the Gemini adapter's deep-research command with typed options. When the `opencli` executable is missing, `OpenCliExecutor` surfaces the failure through the unified exception hierarchy (`OpenCliStartupException` / `OpenCliExecutableFailureException`).

<a id="7-configuration"></a>
## 7. Configuration

Configuration is object-based via `OpenCliProperties`:

| Property | Default | Description |
|---|---|---|
| `executionTarget` | `LOCAL_PROCESS` | `LOCAL_PROCESS` or `REMOTE_AGENT_HTTP` |
| `executable` | `opencli` | Executable name or absolute path |
| `workingDirectory` | — | Subprocess working directory |
| `commandTimeoutMillis` | `300000` | Command timeout (ms; reused as HTTP timeout in remote mode) |
| `maxConcurrentExecutions` | `0` | Max concurrent subprocesses (0 = unlimited) |
| `startupProbeTimeoutMillis` | `30000` | Availability-probe timeout |
| `environment` | `{}` | Extra environment variables (e.g. `OPENCLI_CDP_ENDPOINT`) |
| `remoteAgentBaseUrl` | — | Remote agent base URL |
| `remoteCollectMode` | `cdp` | `bridge` or `cdp` (must match the agent side) |
| `remoteOutputFormat` | `json` | Output format requested from the agent |
| `remoteCdpEndpoint` | — | Optional CDP endpoint overriding the agent default |
| `remoteCaptureRawHttpResponse` | `false` | Keep the raw HTTP body in `OpenCliResult#getRemoteRawHttpBody()` (large responses — use with care) |

Notes:

- In remote mode the `stdout` is the JSON `items` the agent returned (parsed into line items per `format`), which may differ from local raw subprocess text; `leadingArguments` are local-only and are not injected on the remote path.
- `execution-target=REMOTE_AGENT_HTTP` makes the availability probe report `SKIPPED_REMOTE_MODE` (treated as startable).

<a id="8-core-usage"></a>
## 8. Core Usage

### 8.1 meta() and browser()

```java
cli.meta().list("json");
cli.meta().completion("zsh");
cli.browser().session("work", "background").waitFor("selector", ".loaded", null, 10_000L);
```

### 8.2 Center WebSocket reverse agent

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

Protocol: edge sends `register`; center replies `registered`; center sends `collect` (request_id, site, command, args, positional_args, format, mode); the SDK executes `opencli` locally (via `copyForLocalCliExecution()` to avoid loops) and replies `result`; plus `ping` / `pong`.

<a id="9-testing--build"></a>
## 9. Testing & Build

```bash
mvn clean verify
```

- Unit tests cover the adapter registry, core execution, browser/meta/remote paths and WS helpers (19 Java test sources under `src/test`).
- JaCoCo runs `prepare-agent`, `report` and `check` on the `verify` phase with a **90% line-coverage** rule (`haltOnFailure=false`).
- `scripts/generate_opencli_adapter_ids.py` regenerates `OpenCliAdapterIds` / `OpenCliAdapterTaxonomy` from the upstream `opencli/docs/adapters/index.md` + `cli-manifest.json` (set `OPENCLI_ROOT` to point at the upstream tree).
- Release packaging (`mvn -Prelease deploy`) attaches sources and javadoc jars, GPG-signs artifacts and is wired for Sonatype Central Publishing; plain `mvn deploy` routes SNAPSHOT/release artifacts to the Aliyun Maven repository per `distributionManagement`.

<a id="10-versioning--branches"></a>
## 10. Versioning & Branches

| Branch | Version pattern | JDK | Maintenance policy |
|---|---|---|---|
| `feature/1.0.x` (this branch) | `1.0.x.*` | 8 | Compatibility fixes and JDK-8-safe dependency upgrades only |
| `feature/2.0.x` | `2.0.x.*` | 17 | JDK 17 line |
| `feature/3.0.x` (this branch) | `3.0.x.*` | 21 | JDK 21 line |

Branch POMs (JDK and dependency stack per line) are rendered by `scripts/render-branch-pom.py`.

<a id="11-contributing--license"></a>
## 11. Contributing & License

Contributions are welcome. Run `mvn clean verify` before opening a pull request and describe compatibility, testing and migration impact. This project is licensed under the [Apache License 2.0](LICENSE).
