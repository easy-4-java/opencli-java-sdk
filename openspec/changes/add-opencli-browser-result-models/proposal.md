# C07 · Browser 领域结果与严格解析

> Status: DRAFT / REVIEW_REQUIRED · Priority: P1 · Implementation: NOT_STARTED

## Why

AX flags 和 JsonNode typed wrapper 已存在。真正的缺口是按 Browser leaf 的领域结果、解析失败语义与可追溯输出 fixture，而不是再造一组功能相同的参数。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E03, E09, E12, U02, U03；关联问题 F09, F11。源码观察不等于已运行回归测试。

## What Changes

为高价值 Browser leaf 增加 strict decoder 和领域模型，保持现有 raw/JsonNode APIs。不假定所有输出都有统一 envelope，不自动关闭用户绑定标签页。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-browser-result-models`: Browser 领域结果与严格解析的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：C01, C03, C06。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-browser-result-models/spec.md)，任务见 [tasks.md](tasks.md)。
