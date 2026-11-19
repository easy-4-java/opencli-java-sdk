# C03 Tasks · 上游错误协议解析与证据保真

> 错误信封实现和回归已在三条功能分支核对，待 PR 评审与合并。验收证据见下文。

关联规范：[spec](specs/opencli-error-envelope/spec.md)。

## 1. 错误 fixture

- [x] 1.1 从锁定上游 renderError/真实只读负例建立 YAML fixture，保存版本与 hash。见 [上游 stderr 采样](evidence/2026-11-18-upstream-stderr.md)。
- [x] 1.2 补 JSON、纯文本、AutoFix 注释、exit mismatch、unknown code、malformed 和恶意 YAML 失败测试。

## 2. decoder 与结果

- [x] 2.1 选定三线可用的安全 YAML 实现并验证安全配置，不复制不兼容 Jackson imports。见 [三线候选探针](evidence/2026-11-18-yaml-parser-review.md)。
- [x] 2.2 实现有界 shape 校验和 provenance 模型，保留 help/trace/unknown fields。
- [x] 2.3 接入本地异常与 partial result，避免 parser 抢占 primary failure。

## 3. 回归与迁移

- [x] 3.1 验证诊断日志不会输出 parser exception 中的原始敏感片段，配合 C09。
- [x] 3.2 验证写操作不会因解析器或 help 自动重试。
- [ ] 3.3 在三个分支运行共享失败 fixtures，记录真实结果并更新迁移说明。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## OC-ERR-001–006 本地解码与退出来源

`OpenCliResult.errorEnvelope` 在本地失败上提供解码状态、错误码、错误消息、help、trace、扩展字段与独立的 observed/reported 退出码。原始 stderr 保留在结果中。超时、非零退出、输出限额和不完整流继续由执行器决定主异常，由此避免解码覆盖执行证据。成功结果和原有远端 collect 保持各自路径。

JSON 复用各版本线 Jackson，严格检查重复键、尾随内容及嵌套深度。YAML 使用 SnakeYAML 2.7 的事件预检和安全构造器，保留 JSON 标量语义，拒绝锚点、别名、显式标签、重复键及多文档。解码预算为 256 KiB UTF-8、32 层。解析失败仅以状态呈现，原始诊断片段留在 stderr。AutoFix 和 trace 路径作为数据保存。

`OpenCliErrorEnvelopeTest` 首先因缺少 API 无法编译。实现后使用已归档 YAML、合成 JSON 和真实 JVM 子进程验证解码、退出值不一致、输入拒绝、超时优先、诊断脱敏与单次执行。现有远端失败分类继续由 C04 测试覆盖。


## C03 实施核对

- OC-ERR-001 / OC-ERR-002。归档 YAML 和等价 JSON 验证错误码、help、AutoFix 原文、trace 及扩展字段类型，纯文本随退出状态处理。
- OC-ERR-003。真实子进程和等价解码用例核对实际退出 1、报告退出 77 和 mismatch 各自保留。
- OC-ERR-004。回归覆盖输入上限、嵌套深度、重复键、多文档、显式标签、锚点、别名及畸形 JSON/YAML。
- OC-ERR-005。真实进程的非零退出和超时用例验证主异常、部分信封和原始 stderr。新增畸形 YAML 用例同时捕获 SDK 默认诊断和完整异常栈，确认原始 canary 只出现在显式读取的 stderr。
- OC-ERR-006。真实子进程用 marker 计数，验证解析 AutoFix 建议时只执行一次。

本分支以 `5955fc1fdc96a238bfc67f67e453036832a23b09` 为基线，使用 JDK 21 执行 `./mvnw -B -ntp -o -Dtest=OpenCliErrorEnvelopeTest,OpenCliProcessPrivacyTest,OpenCliStdoutJsonTest test`。退出码为 0。21 项实际执行，其中 8 项为错误信封回归，零失败、零错误、零跳过。报告位于 `target/surefire-reports`。
