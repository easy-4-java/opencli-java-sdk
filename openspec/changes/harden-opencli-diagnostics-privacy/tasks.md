# C09 Tasks · 默认日志与异常的隐私边界

> 勾选项已有实现和对应测试证据。文档和格式检查单独记录。

关联规范：[spec](specs/opencli-diagnostics-privacy/spec.md)。

## 1. 泄露负例

- [ ] 1.1 对Executor、AdapterChannel、HTTP、WS、JSON/YAML parser所有日志/异常点建立canary矩阵。
- [x] 1.2 新增完整日志捕获与nested cause测试，先证明当前泄露可被测试检测。

## 2. 安全投影

- [ ] 2.1 将commandLine/args/bodyPreview/remote error输出改成允许表元数据与correlationId。
- [ ] 2.2 实现安全异常摘要和cause处理，避免logger输出provider原始对象。
- [x] 2.3 保留raw业务结果与默认关闭的HTTPdebugcapture，新增显式有限export策略。

## 3. 验证

- [x] 3.1 覆盖DEBUG、timeout、malformedresponse、断线和超限路径，任何canary出现均失败。
- [x] 3.2 验证raw结果内容未被脱敏逻辑无声修改。
- [x] 3.3 三线执行相同隐私矩阵并记录证据；文档标明SDK与应用日志责任边界。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## 2.3 与 3.2 验证记录

- OC-PRIV-003 与 OC-PRIV-005。公开入口为 `OpenCliDiagnosticExport.export(result, maxBytes)`。SDK 在内存中生成允许表投影，保留原始结果的内容，SDK 自身的导出保留时间为零。
- `OpenCliDiagnosticExportTest` 覆盖原值保留、全量与截断、恰好满足预算、预算少一个字节、非法预算和缺失字段。API 缺失时验证失败，边界回归随后复现截断标记不一致。修复后全部通过。现有 HTTP 测试也验证原报文的显式捕获和默认关闭。
- 1.0.x 功能分支提交 `295f1d32d2b00f7f3d1ae74eb4f95d792b309d9c`，JDK 8，`mvn -B --no-transfer-progress clean verify`，2016 个测试。
- 2.0.x 功能分支实现提交 `72abff8d0d09918922886aef364a3b85c14f4d09`，JDK 17，`mvn -B --no-transfer-progress verify`，2087 个测试。
- 3.0.x 功能分支提交 `6fa874a3c811733126350ca75beb750f78ba8956`，JDK 21，`bash ./mvnw -B --no-transfer-progress clean verify`，2016 个测试。
- 三线本地构建退出码均为 0，失败、错误和跳过均为 0。报告位于各构建目录的 `target/surefire-reports`。其余场景和跨平台验收仍由相应任务跟踪。

## 2026-11-19 C09 默认异常链与三线隐私矩阵

JDK 17 两项定向红灯实际退出 1。选项对象的 `toString()` 抛出带嵌套 cause 和 suppressed 项的异常，栈文本泄露测试密钥。真实缺失可执行文件的低层 result handler 异常链泄露路径。修复后，值转换报告固定字段诊断，result handler 保留安全摘要。显式 `RunSession.getIoFailure()` 仍返回原始低层诊断。两项回归都扫描完整栈文本，保留原始数据的测试同时验证权限边界。

| 场景 | 现有三线回归 |
| --- | --- |
| Adapter DEBUG、本地非零退出、超时、超限、原始 stdout/stderr | `OpenCliProcessPrivacyTest` |
| 参数对象的嵌套 cause/suppressed、底层原生路径 | `OpenCliArgSupportTest`、`SubprocessExecutionSupportTest` |
| HTTP 503、凭据 URL、畸形 JSON、原文捕获开关 | `OpenCliRemoteAgentHttpClientTest` |
| WebSocket 断线和投递异常、配置 URL、日志测试密钥 | `OpenCliWsReverseAgentSessionTest`、`OpenCliWsReverseAgentClientTest` |
| JSON 解析 DEBUG、默认不写入的有界导出 | `OpenCliStdoutJsonTest`、`OpenCliDiagnosticExportTest` |
| 启动日志的可用性摘要，配置和探测原始值的显式读取 | `OpenCliAvailabilityReportTest` |

JDK 17 使用系统 Maven 执行 `-B -ntp -o clean verify`，2,134 项通过，0 失败、0 错误、0 跳过。中英文 README 记录应用显式访问原始低层诊断的责任。C03 的 YAML 解码器仍待实施，对其解析日志的验证保留在后续矩阵中。

## 启动诊断摘要验证

JDK 17 基线 `0cbb3ee750062675813f1d3389c5f7f14b50345c` 的定向用例在 3 项中失败 1 项。面向日志的 `toDiagnosticMessage()` 输出了配置值、解析后的可执行文件路径和原始探测消息。修改后，该方法只输出就绪状态和状态码。用例继续断言 `getConfiguredExecutable()`、`getResolvedExecutablePath()` 和 `getMessage()` 保留原始值。

三条线使用其 Maven Wrapper 运行 `./mvnw -B -ntp clean verify`，报告位于各自的 `target/surefire-reports`。Java 8 提交 `49250f97cb6618502b40e5989b8521fc733d7fab`、Java 17 本提交和 Java 21 提交 `7001ed85439408f9d2724e7edee92d3cfc7853e9` 分别通过 2178、2179、2178 项，均为零失败、零错误、零跳过。
