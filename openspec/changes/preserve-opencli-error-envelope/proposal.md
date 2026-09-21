# C03 · 上游错误协议解析与证据保真

> Status: DRAFT / REVIEW_REQUIRED · Priority: P0 · Implementation: NOT_STARTED

## Why

当前 SDK 主要识别 JSON 外形；上游 Adapter 错误使用 stderr YAML，wire 字段为 help，可附 trace。只解析 stdout JSON 将漏掉最重要的失败信息。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E01, E08, E09, U04, U05；关联问题 F05。源码观察不等于已运行回归测试。

## What Changes

增加错误 decoder、错误来源与 partial-result 保真；兼容 JSON 和纯文本。不得把所有 stderr 输出都视作错误，也不自动重试命令。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-error-envelope`: 上游错误协议解析与证据保真的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：C02。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-error-envelope/spec.md)，任务见 [tasks.md](tasks.md)。
