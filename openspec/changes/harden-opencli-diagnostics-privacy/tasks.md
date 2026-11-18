# C09 Tasks · 默认日志与异常的隐私边界

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-diagnostics-privacy/spec.md)。

## 1. 泄露负例

- [ ] 1.1 对Executor、AdapterChannel、HTTP、WS、JSON/YAML parser所有日志/异常点建立canary矩阵。
- [x] 1.2 新增完整日志捕获与nested cause测试，先证明当前泄露可被测试检测。

## 2. 安全投影

- [ ] 2.1 将commandLine/args/bodyPreview/remote error输出改成允许表元数据与correlationId。
- [ ] 2.2 实现安全异常摘要和cause处理，避免logger输出provider原始对象。
- [ ] 2.3 保留raw业务结果与默认关闭的HTTPdebugcapture，新增显式有限export策略。

## 3. 验证

- [x] 3.1 覆盖DEBUG、timeout、malformedresponse、断线和超限路径，任何canary出现均失败。
- [ ] 3.2 验证raw结果内容未被脱敏逻辑无声修改。
- [x] 3.3 三线执行相同隐私矩阵并记录证据；文档标明SDK与应用日志责任边界。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## 2026-11-19 C09 默认异常链与三线隐私矩阵

JDK 17 两项定向红灯实际退出 1。选项对象的 `toString()` 抛出带嵌套 cause 和 suppressed 项的异常，栈文本泄露测试密钥。真实缺失可执行文件的低层 result handler 异常链泄露路径。修复后，值转换报告固定字段诊断，result handler 保留安全摘要。显式 `RunSession.getIoFailure()` 仍返回原始低层诊断。两项回归都扫描完整栈文本，保留原始数据的测试同时验证权限边界。

| 场景 | 现有三线回归 |
| --- | --- |
| Adapter DEBUG、本地非零退出、超时、超限、原始 stdout/stderr | `OpenCliProcessPrivacyTest` |
| 参数对象的嵌套 cause/suppressed、底层原生路径 | `OpenCliArgSupportTest`、`SubprocessExecutionSupportTest` |
| HTTP 503、凭据 URL、畸形 JSON、原文捕获开关 | `OpenCliRemoteAgentHttpClientTest` |
| WebSocket 断线和投递异常、配置 URL、日志测试密钥 | `OpenCliWsReverseAgentSessionTest`、`OpenCliWsReverseAgentClientTest` |
| JSON 解析 DEBUG、默认不写入的有界导出 | `OpenCliStdoutJsonTest`、`OpenCliDiagnosticExportTest` |

JDK 21 使用仓库 Maven Wrapper 执行 `-B -ntp -o clean verify`，2,133 项通过，0 失败、0 错误、0 跳过。中英文 README 记录应用显式访问原始低层诊断的责任。畸形 YAML 的验证见 C03 的隐私回归及下文。


## 本地失败诊断关联

OC-PRIV-001/004。OpenCliProcessPrivacyTest 的新用例在 Java 17 基线复现缺少 correlationId，退出 1。现在本地进程失败时由 SDK 生成随机 UUID，在 DEBUG 日志及返回的异常消息中使用同一值。日志只记录终止原因、退出码和捕获字节数。测试扫描捕获的日志和完整异常栈，同时通过 partialResult 校验 stderr 原值。C03 的 OpenCliErrorEnvelopeTest 覆盖含敏感标记的畸形 YAML，并确认解析器原文不进入默认诊断。

Java 8、17、21 分别用各自 Wrapper 运行 OpenCliProcessPrivacyTest、OpenCliErrorEnvelopeTest 和 OpenCliProcessContractTest，每条线执行 31 项，零失败、零错误、零跳过。报告位于 target/surefire-reports。Java 21 的先前运行曾出现继承管道用例的 JUnit 临时目录清理竞态，本次重跑通过。该竞态需要独立处理。
