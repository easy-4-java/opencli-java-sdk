# C01 · 参数保真与统一调用契约

> Status: DRAFT / REVIEW_REQUIRED · Priority: P0 · Implementation: NOT_STARTED

## Why

三个分支的 Executor 会 trim 并丢弃空白参数，AdapterChannel 也有同类处理。已有通用 Adapter API，应修复它而非重复建设。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E01, E02, E05, E17, E18；关联问题 F01。源码观察不等于已运行回归测试。

## What Changes

修改参数构造和校验；保留 List、varargs、结构化请求及现有 Client 门面；不新增 shell 执行模式，不扩展站点功能。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-argv-contract`: 参数保真与统一调用契约的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：无硬实现前置；可先完成本 Change 的测试/基础工作。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-argv-contract/spec.md)，任务见 [tasks.md](tasks.md)。
