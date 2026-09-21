# C09 · 默认日志与异常的隐私边界

> Status: DRAFT / REVIEW_REQUIRED · Priority: P0 · Implementation: NOT_STARTED

## Why

当前异常与日志会打印commandLine、参数摘要、HTTP body preview和远端error，可能泄露提示词、代码和凭据。上游抓包脱敏不解决Java日志问题。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E01, E02, E06, E07, E09；关联问题 F07。源码观察不等于已运行回归测试。

## What Changes

修复SDK默认诊断投影、异常公开文本与显式导出策略，保留已授权调用者的raw业务数据。不是内容审查，也不无声删改执行结果。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-diagnostics-privacy`: 默认日志与异常的隐私边界的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：无硬实现前置；可先完成本 Change 的测试/基础工作。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-diagnostics-privacy/spec.md)，任务见 [tasks.md](tasks.md)。
