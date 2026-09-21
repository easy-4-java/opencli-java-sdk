# C05 · 反向 Worker 状态机、背压与去重

> Status: DRAFT / REVIEW_REQUIRED · Priority: P0 · Implementation: NOT_STARTED

## Why

现有反向 WebSocket Client 已实现接单执行，但 cached pool 无界、注册 gate 和去重不足、关闭不等待任务。必须补足 Worker 边界，而不是假设它已是应用端 WS RPC。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E07, E01, E04；关联问题 F06。源码观察不等于已运行回归测试。

## What Changes

维持 register/registered/collect/result/ping/pong 协议，增加本地状态机、有限任务治理、代际和去重。不给服务器强加新重放协议，不保证跨重启 exactly-once。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-reverse-agent`: 反向 Worker 状态机、背压与去重的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：C02, C03。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-reverse-agent/spec.md)，任务见 [tasks.md](tasks.md)。
