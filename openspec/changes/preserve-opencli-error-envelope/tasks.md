# C03 Tasks · 上游错误协议解析与证据保真

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-error-envelope/spec.md)。

## 1. 错误 fixture

- [ ] 1.1 从锁定上游 renderError/真实只读负例建立 YAML fixture，保存版本与 hash。
- [ ] 1.2 补 JSON、纯文本、AutoFix 注释、exit mismatch、unknown code、malformed 和恶意 YAML 失败测试。

## 2. decoder 与结果

- [ ] 2.1 选定三线可用的安全 YAML 实现并验证安全配置，不复制不兼容 Jackson imports。
- [ ] 2.2 实现有界 shape 校验和 provenance 模型，保留 help/trace/unknown fields。
- [ ] 2.3 接入本地异常与 partial result，避免 parser 抢占 primary failure。

## 3. 回归与迁移

- [ ] 3.1 验证诊断日志不会输出 parser exception 中的原始敏感片段，配合 C09。
- [ ] 3.2 验证写操作不会因解析器或 help 自动重试。
- [ ] 3.3 在三个分支运行共享失败 fixtures，记录真实结果并更新迁移说明。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。
