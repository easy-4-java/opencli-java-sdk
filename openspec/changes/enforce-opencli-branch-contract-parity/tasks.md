# C10 Tasks · 共享契约测试、分支差异与上游锁定

> 已验收的准备任务见下方证据。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-branch-contract-parity/spec.md)。

## 1. 基础阶段可先行

- [ ] 1.1 记录三个精确HEAD和允许差异矩阵，确定first-implementation分支经评审认可。
- [x] 1.2 建立共享contractfixture目录、来源lock与测试runner，复用现有coverage而不删除其它测试。
- [x] 1.3 定义完整argv/协议/typed/live分层报告，加入0测试和missingreport失败规则。见本文真实安装版和报告反例。

## 2. 上游与文档

- [x] 2.1 采集锁定上游的实际help/manifest/output，保存hash与范围，移除对作者绝对路径的依赖。
- [x] 2.2 修正各分支README/POMdescription/Gradle示例，检查Java/Jackson/Maven实际声明。
- [x] 2.3 配置三线正确wrapper/JDK的独立构建与报告，不通过修改coverage阈值绕开行为问题。

## 3. 最终集成门禁

- [ ] 3.1 在C01–C09选定实现完成后，分别在三线执行全部对应正负fixtures，记录HEAD与真实退出结果。
- [x] 3.2 运行官方OpenSpec逐Change与all strict校验，保存CLI版本和报告；环境失败保持NOT_RUN。
- [ ] 3.3 核对Requirement到测试/commit的闭环并进行独立代码评审，才允许勾选和archive。
- [ ] 3.4 提交/推送/PR/发布仅在用户明确授权时执行；观察实际CI，不承诺未发生的远端结果。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

### 2.2 与 2.3 的三线验收

OC-PARITY-005：以表中检出的三条线为准，逐条核对中英文 README 的 Maven 和 Gradle 坐标、POM 描述与 Java/Jackson 声明，以及 Wrapper 的分发版本。三个版本的坐标均为 `io.github.easy4j:opencli-java-sdk`，分别使用 `1.0.x.20260630-SNAPSHOT`、`2.0.x.20260630-SNAPSHOT` 和 `3.0.x.20260630-SNAPSHOT`。

OC-PARITY-004：维护线原 CI 将 JaCoCo 参数丢在 Surefire JVM 之外，构建成功时仍缺执行数据。修复后的各线 CI 使用其 Wrapper 执行 `./mvnw -B --no-transfer-progress clean verify`。三次退出码均为 0，Surefire XML 的失败、错误、跳过数均为 0，且均上传了 `surefire-reports` 和 `jacoco-coverage`。三条线保留 90% 行覆盖率告警阈值，并要求上传 JaCoCo 报告。

| JDK | Wrapper Maven | Jackson | 构建提交 | 测试数 | 行覆盖率 | GitHub CI |
| --- | --- | --- | --- | ---: | ---: | --- |
| 8 | 3.9.16 | 2.18.9 | `aaa295fc6e65708554ab7bb55cf7c05fb990f6ae` | 2144 | 3043/3303（92.13%） | [44356116353](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44356116353) |
| 17 | 3.9.16 | 2.22.1 | `cf9360e99647c935d7ee10c87a391ed602647682` | 2145 | 3030/3286（92.21%） | [44355648596](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44355648596) |
| 21 | 4.0.0-rc-5 | 3.2.1 | `fa7e001da203d31fd5afb2921f90c3f428db2d5b` | 2144 | 3028/3285（92.18%） | [44356116107](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44356116107) |

上述记录覆盖文档事实和分支构建。最终集成门禁仍按第 3 节逐项验收。

### 结构化帮助证据门禁

OC-PARITY-003/004：在 `98f28346e7bc3307608122a0086a8e1072e39536` 的 GitHub 工件中，Surefire 记录了三个通过的 `OpenCliStructuredHelpFixtureTest` 用例，契约报告的枚举层仍显示 `NOT_RUN`。从该工件重放报告命令并声明枚举套件后，枚举层显示 `PASS`、执行三项，真实 CLI 执行层仍为 `NOT_RUN`。移除套件报告时，报告命令返回非零。三条开发线使用同一份 SHA-256 清单检查九个帮助样本，在临时副本的 `root.json` 末尾追加换行会使校验失败。样本提供离线结构证据，实时能力发现和真实执行由对应任务另行验收。

### 每个必选套件的执行证据

OC-PARITY-003/004：当同一层含一个已执行套件及一个零用例套件时，旧报告返回 PASS。把同一套件注册到 argv 与 real-execution 也会重复记账。新增两个失败回归后，报告逐套件检查非零执行、零跳过、零失败及零错误，并拒绝同一套件跨层归因。问题套件列在 `unprovenSuites`。

JVM 和平台报告显式登记错误报文、浏览器 typed 结果、上下文和诊断测试。Windows npm 安装版检查用独立 `installed-report.json`，保留实际 Node、OpenCLI、Java 和 Maven 版本、源码 SHA 与执行退出码。真实 CLI 层的范围为安装版 launcher 与隔离的 help、插件和诊断测试。普通 JVM 或平台报告缺少此层时保持 NOT_RUN。用于 Browser 结果解析的全部归档样本继续按 SHA-256 验证。

### OC-PARITY-003 运行间证据隔离

在 `6da93d05` 用两个旧的通过报告和一个成功退出但未执行测试的 Maven 替身复现错误通过。安装契约脚本现在在启动 Maven 前将已有 Surefire 报告归档到 `platform-evidence/pre-install-surefire.*`，本次门禁只读取重新生成的 `target/surefire-reports`。回归中真实执行层从错误的 `PASS` 改为 `FAIL`，执行数为 0，退出码为 1，旧报告仍可回查。

在 `e858cc0`、`6da93d05`、`c22fe45` 加入修复后，三线的 `python3 -m unittest discover -s scripts/tests` 各通过 16 项。使用本地缓存的已安装 OpenCLI 1.8.8 依赖运行同一脚本，Java 8、17、21 分别用本线 Wrapper 实际执行覆盖和只读诊断集成测试，各通过 2 项，零跳过。报告为 `platform-evidence/installed-report.json`。

### OC-PARITY-002/004 共享来源锁定与三线报告核对

基线 Java 8 `876a48a`、Java 17 `004c3286`、Java 21 `a384a5b` 的托管 JVM 报告分别记录 2231、2232、2231 项执行，零失败、零错误、零跳过。对应 Windows 工件的真实安装层各执行 4 项，零跳过。Windows 进程层分别记录 40、38、40 项。Java 17 文件树包含 `SubprocessStreamFailureTest`，但该套件未进入它的平台执行和报告配置。补齐后，JVM 和平台报告都要求该套件执行。

Java 21 基线的共享校验清单引用了未被 Git 跟踪的 `sources.lock.json`，`shasum -a 256 -c SHA256SUMS` 返回非零。从 Java 17 基线恢复相同的来源锁定文件后，三条线均校验 28 个文件。临时副本中追加一个换行会使校验退出 1。三条常规 CI 在 Maven 构建前校验共享来源锁定、结构化帮助及浏览器输出三份清单。

使用 OpenSpec 1.14.1 在三条基线逐项运行 `openspec validate <change> --strict --no-interactive` 并执行 `openspec validate --all --strict --no-interactive`，每条线均通过 10 个变更，退出 0。Java 17 基线的 [托管工件](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44382023710) 另记录固定版本 1.13.1 的逐项结果和 `all.log`，同样通过。此处记录规格校验与工件事实。独立评审与归档仍按最终门禁验收。

### 2026-11-19 上游 manifest 采集

OC-PARITY-002：从 npm 发布包提取 `package/cli-manifest.json`。源为 https://registry.npmjs.org/@jackwener/opencli/-/opencli-1.8.8.tgz，包的 SHA-512 完整性与 npm registry 元数据一致。原始 manifest 的 SHA-256 为 `b6f44ab5973dfd46b001f42562ad520b184bf1dd27ec4ae807f849df70b98e6a`。范围为发布包自带的 adapter 命令枚举，共 1,366 条、179 个 site。按 `(site, name)` 逐项对比仓库历史快照，新增 89 条，移除 0 条。89 条新增命令的逐项差异保存在本节链接的来源锁定中。

在临时目录采集并运行生成器后，报告只记录公开来源、原始字节的 SHA-256、观察日期、获取命令和枚举范围。历史报告仍用于原有 Java 测试。生成器通过 `--manifest` 或 `OPENCLI_MANIFEST` 读取本地输入，移植验证覆盖含空格的临时路径。

```bash
curl -fsSLo opencli-1.8.8.tgz https://registry.npmjs.org/@jackwener/opencli/-/opencli-1.8.8.tgz
tar -xOf opencli-1.8.8.tgz package/cli-manifest.json > cli-manifest.json
```


### OpenCLI 1.8.8 全量样本复跑

[上游来源锁定](../../../src/test/resources/opencli-contracts/v1/upstream-1.8.8.lock.json) 记录 manifest 采集命令及逐项差异，以及帮助和浏览器输出的校验清单与复跑入口。使用 Node 24.18.0 和已安装的 1.8.8 包，在独立临时配置、缓存及用户目录中执行。11 份帮助、12 份浏览器叶子输出均与已归档的 stdout 逐字节相同，全部退出 0，stderr 为空。帮助包含非 JSON 输出。浏览器叶子使用真实 CLI 与隔离页面桩，真实浏览器联调证据单独见 [Browser Bridge 验收](../add-opencli-browser-result-models/evidence/2026-11-19-real-browser-bridge.md)。历史覆盖基线的升级仍须评审。

### 2026-11-19 三线契约与追溯记录

以下提交为本次验证输入。三条线在 macOS arm64 使用各自 Wrapper 运行 `./mvnw -B -ntp -o clean verify`。退出码均为 0，Surefire 零失败、零错误、零跳过。GitHub 链接指向同一 SHA 的独立托管运行，具体执行平台和结果以运行报告为准。

| JDK | Jackson | Maven Wrapper | 验证提交 | 全量测试 | JaCoCo 行覆盖率 | 托管运行 |
| --- | --- | --- | --- | ---: | ---: | --- |
| 8u504 | 2.18.9 | 3.9.16 | `8da04d3fe92dfc03572e6fb1d710262753309250` | 2,240 | 3,913/4,236（92.37%） | [Intel macOS、Windows](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44399149359) |
| 17.0.21 | 2.22.1 | 3.9.16 | `4fb8c735c38ebd6f30311d4b163a084f6cc4a377` | 2,241 | 3,908/4,227（92.45%） | [Linux 构建](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44399165709) · [契约矩阵](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44399150013) |
| 21.0.13.0.2 | 3.2.1 | 4.0.0-rc-5 | `df3b58cb4dc24d5f270d78e889006a621b99826d` | 2,240 | 3,907/4,226（92.45%） | [两种 macOS、Windows](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44399148656) |

使用 `scripts/contract_report.py` 和 Java 17 JVM 契约工作流的同一组 18 个必选套件，读取三份 `target/surefire-reports`。各线均执行枚举 10 项、argv 48 项、进程 43 项、协议 93 项、类型化结果 16 项，各层零跳过。真实 CLI 层在本地全量报告中为 `NOT_RUN`。本地全量和托管平台分别核对，实际 OpenCLI 安装版、Browser Bridge 和服务端能力以各变化的专项证据为准。各线也通过了三份样本清单的 30 项 SHA-256 校验。

| 规格 | 可回查的测试与范围 |
| --- | --- |
| [C01](../harden-opencli-argv-contract/tasks.md) OC-ARGV-001–005 | `OpenCliArgvContractTest`、`OpenCliStructuredArgvContractTest`。子进程实际观察 argv，包括空字符串、有序重复选项、显式 false、分隔符以及拒绝非法组合。完整差异的合入评审仍开放。 |
| [C02](../harden-opencli-process-lifecycle/tasks.md) OC-PROC-001–006 | `OpenCliProcessContractTest`、`OpenCliProcessBoundaryTest`、`SubprocessCleanupFaultTest`、`SubprocessCompletionRaceTest`、`SubprocessStreamFailureTest` 及 `SubprocessExecutionSupportTest`。覆盖取消、超限、等待、清理不确定性、迟到的读取失败和旧输出流兼容。真实 Browser Bridge 的有界 follow 见 C02 的专项记录。未知后代的回收边界仍开放。 |
| [C03](../preserve-opencli-error-envelope/tasks.md) OC-ERR-001–006 | `OpenCliErrorEnvelopeTest`。覆盖 YAML、JSON、未知字段、退出来源、畸形输入、原始 stderr 以及主失败和禁止重复写操作的行为。 |
| [C04](../harden-opencli-remote-collect/tasks.md) OC-REMOTE-001–006 | `OpenCliRemoteAgentHttpClientTest`。回环 HTTP/TLS 验证转换拒绝、响应预算、超时、认证与异常字符集，保留实际 HTTP 状态。远端扩展需要独立的服务端契约。 |
| [C05](../harden-opencli-reverse-agent/tasks.md) OC-WS-001–006 | `OpenCliWsReverseAgentSessionTest`、`OpenCliWsCollectRequestsTest`。本地 WS 夹具覆盖注册门限、限流、去重、代际和关闭。 |
| [C06](../add-opencli-capability-discovery/tasks.md) OC-CAP-001–006 | `OpenCliStructuredHelpFixtureTest`、`OpenCliHelpParserTest`、`OpenCliCapabilityDiscoveryTest`、`OpenCliCapabilityCommandTest`。覆盖真实帮助样本、scope、会话占位、快照和有界探测。真实覆盖范围以 C06 样本和运行时证据为准。 |
| [C07](../add-opencli-browser-result-models/tasks.md) OC-BROWSER-001–006 | `OpenCliBrowserResultTest`、`OpenCliBrowserEvidenceResultTest`。覆盖声明的叶子、透明字段、大 ID、严格失败、AX 选项和旧返回类型。真实浏览器验证见 C07 的 Bridge 证据。 |
| [C08](../add-opencli-context-and-diagnostics/tasks.md) OC-CTX-001–006 | `OpenCliInvocationContextTest`、`OpenCliDiagnosticsTest`。覆盖并发调用隔离、scope、诊断未知状态、只读动作和 sitemap 数据边界。 |
| [C09](../harden-opencli-diagnostics-privacy/tasks.md) OC-PRIV-001–006 | `OpenCliProcessPrivacyTest`、`OpenCliDiagnosticExportTest`、`OpenCliErrorEnvelopeTest`、HTTP/WS 回归。覆盖已登记的日志、异常链和有界导出标记。完整日志点清册及其评审仍开放。 |
| [C10](specs/opencli-branch-contract-parity/spec.md) OC-PARITY-001–006 | 上述精确 SHA、分支工具链矩阵、清单校验、分层报告及历史真实执行记录。OpenSpec strict 校验只证明规格有效。最终验收依据第 3 节。 |

Java 17 的独立定向评审以 `c6d544638e44dba3c53ec3e5a6f65f2f20a16340` 的 SDK 源码为准。基线已编译的调用方可链接旧签名、读取并修改有界输出视图。该视图的后续写入不会改变原执行证据。独立探针确认读取异常与子进程退出同时发生时保留 `IO_FAILURE`，以及 HTTP 200/503 遇到无效字符集时分别保留协议失败及 HTTP 失败。Java 17 本次验证提交在该源码之上增加 CI 和规格记录。此次复审只覆盖这些具体修复，最终全量评审和归档仍开放。


### OC-PARITY-003/004 真实安装版和报告反例

独立于全量构建，使用已从 npm 取得的 OpenCLI 1.8.8 和 Node 24.18.0，运行 `OpenCliCapabilityOverrideIT` 与 `OpenCliReadOnlyDiagnosticsIT`。Java 8 和 21 使用上表验证提交，Java 17 使用 `c4a6646b9c8592bfae17cc641c74e031f4bcb7f4`，其 SDK 源码与上表一致。三线各自使用 Wrapper，旧 Surefire 报告先移出测试目录。每线真实执行层均为 `PASS`，各执行 2 项，零失败、零错误、零跳过，命令和报告退出 0。范围为隔离的插件发现及只读诊断。

用各线报告的独立副本做两项反例。移除 `OpenCliReadOnlyDiagnosticsIT` 报告时，该套件出现在 `missingSuites`。保留报告文件但将实测用例数置零时，该套件出现在 `unprovenSuites`。六次报告均在另一套件通过且 Maven 退出 0 的输入下返回 `FAIL`，报告命令退出 1。报告以必选套件和执行层分别判定。平台覆盖和最终集成验收继续按第 3 节执行。


### OC-PARITY-003 能力发现的必选报告

独立评审确认 GitHub 的 JVM 报告漏列 `OpenCliCapabilityDiscoveryTest` 和 `OpenCliCapabilityCommandTest`。用原工作流的四组报告参数逐一重放，任一个能力套件的 XML 缺失、零用例或全跳过时，24 个反例仍返回成功。

三线平台作业现在显式运行两个套件，在枚举层登记 DiscoveryTest，在 argv 层登记 CommandTest。Java 17 的完整 JVM 报告也登记这两个套件。使用现有 Surefire 结果重放更新后的四组参数，完整报告均退出 0，24 个反例均退出 1，准确标识缺失或未执行的套件。各线的报告脚本回归均通过 17 项。

### 最终候选的逐项追溯

[需求、测试方法、实现提交与三线候选工件](evidence/final-requirement-trace.md) 按 59 个 Requirement 标识逐项记录。勾选最终门禁时，以最新候选的实际托管结果和独立整合评审为准。
