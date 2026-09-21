# C02 · 有界执行、排队超时与进程清理

> Status: DRAFT / REVIEW_REQUIRED · Priority: P0 · Implementation: NOT_STARTED

## Why

静态 Semaphore 会被新 Client 替换；排队无超时，stdout/stderr 无界，中断路径缺少明确清理。现有 watchdog 并不能证明全部资源有界。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E01, E04, E12；关联问题 F02, F03。源码观察不等于已运行回归测试。

## What Changes

限制单次执行、队列和 capture；定义 cancel/timeout 的所有权和清理结果。保留同步 API；不实现持久化工作流或替换共享 OpenCLI daemon。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-process-lifecycle`: 有界执行、排队超时与进程清理的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：C01。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-process-lifecycle/spec.md)，任务见 [tasks.md](tasks.md)。
