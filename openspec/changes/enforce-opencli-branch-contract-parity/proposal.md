# C10 · 共享契约测试、分支差异与上游锁定

> Status: DRAFT / REVIEW_REQUIRED · Priority: P0/P1 · Implementation: NOT_STARTED

## Why

原始审计快照中，1.x有五个coverage类，2.x/3.x对应目录未出现；提交前远端2.x/3.x已补回套件，当前3.x metadata为1277命令/173站点，来源已不再是作者绝对路径。见 [提交前复核补记](../../../docs/architecture/opencli-java-sdk-publish-check.md)。后续复用现有套件并核对其行为，不重复恢复、不删除现有测试；仍需完整来源锁定、参数语义及真实执行证据。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E10, E11, E12, E13, E14, E15, E16, E17, E18；关联问题 F12, F13。源码观察不等于已运行回归测试。

## What Changes

建立共享contract vectors、上游来源锁定、三线允许差异和真实CI验收；修正README/POM事实。分基础阶段和最终集成门禁，不实施站点业务能力。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-branch-contract-parity`: 共享契约测试、分支差异与上游锁定的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：无硬实现前置；可先完成本 Change 的测试/基础工作。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-branch-contract-parity/spec.md)，任务见 [tasks.md](tasks.md)。
