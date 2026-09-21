# C08 · 隔离执行上下文与只读诊断

> Status: DRAFT / REVIEW_REQUIRED · Priority: P1 · Implementation: NOT_STARTED

## Why

上游有profile/window/site-session/keep-tab/trace等上下文；并发使用时不能改全局默认。已有auth/skills/meta可复用，诊断结果也不能把远程跳过当作健康。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E12, E03, U02, U03, U05, U01, U06；关联问题 F10, F11。源码观察不等于已运行回归测试。

## What Changes

新增不可变单次上下文、保守只读诊断与可选sitemap提示集成。完整sitemap文件管理、自动修复和Java pipeline执行均暂缓。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-context-diagnostics`: 隔离执行上下文与只读诊断的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：C04, C06, C07。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-context-diagnostics/spec.md)，任务见 [tasks.md](tasks.md)。
