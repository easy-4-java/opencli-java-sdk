# C06 Tasks · 结构化能力发现与可追溯快照

> 勾选项有对应的执行证据。

关联规范：[spec](specs/opencli-capability-discovery/spec.md)。

## 1. schema基线

- [x] 1.1 从锁定 OpenCLI 采集 root、browser、meta 和 site 帮助，保存原文与 SHA-256。不能读取为单一 JSON 文档的输出单列为负例。见 [1.8.8 采集证据](evidence/2026-11-19-structured-help.md)。
- [ ] 1.2 为scope、optionalvalue、negate、false、aliases、session占位和旧格式编写失败测试。

## 2. 服务实现

- [ ] 2.1 新增Java8可表达schema与快照模型，保留unknown字段和raw；复用已有adapter入口。
- [ ] 2.2 实现有限version/help探测与可信executable校验，禁止自动业务操作。
- [ ] 2.3 实现cache key、内容hash、失效与UNKNOWN/STALE/UNSUPPORTED原因。

## 3. 边界验证

- [ ] 3.1 测试本地override/插件来源未知时不伪造来源和全量覆盖率。
- [ ] 3.2 测试原有raw入口不会因为发现故障被无声禁用，remote拒绝范围仍受C04控制。
- [ ] 3.3 三线同一schema fixture结果一致；报告未运行的真实上游采集项。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。
