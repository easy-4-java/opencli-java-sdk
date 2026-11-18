# C03 Tasks · 上游错误协议解析与证据保真

> 已采集上游 stderr 样本，decoder 实现待评审。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-error-envelope/spec.md)。

## 1. 错误 fixture

- [x] 1.1 从锁定上游 renderError/真实只读负例建立 YAML fixture，保存版本与 hash。见 [上游 stderr 采样](evidence/2026-11-18-upstream-stderr.md)。
- [ ] 1.2 补 JSON、纯文本、AutoFix 注释、exit mismatch、unknown code、malformed 和恶意 YAML 失败测试。

## 2. decoder 与结果

- [ ] 2.1 选定三线可用的安全 YAML 实现并验证安全配置，不复制不兼容 Jackson imports。见 [三线候选探针](evidence/2026-11-18-yaml-parser-review.md)。
- [ ] 2.2 实现有界 shape 校验和 provenance 模型，保留 help/trace/unknown fields。
- [ ] 2.3 接入本地异常与 partial result，避免 parser 抢占 primary failure。

## 3. 回归与迁移

- [ ] 3.1 验证诊断日志不会输出 parser exception 中的原始敏感片段，配合 C09。
- [ ] 3.2 验证写操作不会因解析器或 help 自动重试。
- [ ] 3.3 在三个分支运行共享失败 fixtures，记录真实结果并更新迁移说明。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## OC-ERR-001–006 本地解码与退出来源

`OpenCliResult.errorEnvelope` 在本地失败上提供解码状态、错误码、错误消息、help、trace、扩展字段与独立的 observed/reported 退出码。原始 stderr 保留在结果中。超时、非零退出、输出限额和不完整流继续由执行器决定主异常，由此避免解码覆盖执行证据。成功结果和原有远端 collect 保持各自路径。

JSON 复用各版本线 Jackson，严格检查重复键、尾随内容及嵌套深度。YAML 使用 SnakeYAML 2.7 的事件预检和安全构造器，保留 JSON 标量语义，拒绝锚点、别名、显式标签、重复键及多文档。解码预算为 256 KiB UTF-8、32 层。解析失败仅以状态呈现，原始诊断片段留在 stderr。AutoFix 和 trace 路径作为数据保存。

`OpenCliErrorEnvelopeTest` 首先因缺少 API 无法编译。实现后使用已归档 YAML、合成 JSON 和真实 JVM 子进程验证解码、退出值不一致、输入拒绝、超时优先、诊断脱敏与单次执行。现有远端失败分类继续由 C04 测试覆盖。
