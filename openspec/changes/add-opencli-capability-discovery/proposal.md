# C06 · 结构化能力发现与可追溯快照

> Status: DRAFT / REVIEW_REQUIRED · Priority: P1 · Implementation: NOT_STARTED

## Why

已有通用 Adapter 入口，但缺少基于实际上游 Help 的参数契约与可追溯快照。不能继续把手写常量和未标版本的1275条fixture当成动态全集。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E02, E10, U02, U03, U05；关联问题 F08。源码观察不等于已运行回归测试。

## What Changes

新增 capability service、schema模型与受限发现缓存，复用 adapter(id)。不自动生成全部返回DTO，不验证每个网站账号，不安装或运行插件业务命令。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-capability-discovery`: 结构化能力发现与可追溯快照的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：C01, C02, C03, C04。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-capability-discovery/spec.md)，任务见 [tasks.md](tasks.md)。
