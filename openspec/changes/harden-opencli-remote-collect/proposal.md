# C04 · Legacy HTTP collect 边界与保真

> Status: DRAFT / REVIEW_REQUIRED · Priority: P0 · Implementation: NOT_STARTED

## Why

argv 转 site/command/Map 的旧协议会丢失重复选项、空值、负数等信息，也无法默认表达 Browser session 或任意 root 命令。当前 HTTP 返回的 0/1 是映射值，不是已观察的远端退出码。

依据：[源码复核证据](../../../docs/architecture/opencli-java-sdk-evidence.md)中的 E05, E06, U03；关联问题 F04, F05。源码观察不等于已运行回归测试。

## What Changes

明确 opencli-admin legacy collect 子集和响应语义；小范围隔离 transport 能力。保留已有 wire DTO，不新建未获服务端支持的 endpoint 或 WS RPC。

本 Change 定义可验收行为和失败边界。实现必须先写能暴露现有缺陷的测试，再做最小修改。不得把文档生成作为实现完成。

## Capabilities

### New Capabilities

- `opencli-remote-collect`: Legacy HTTP collect 边界与保真的首次正式规格；已有底层能力只做修复/增量增强，不宣称从零新增。

### Modified Capabilities

本轮未确认有可直接修改的已生效根规格。本包先以 ADDED 形式提供提案，不提前写入 `openspec/specs/`；应用时若已有相关根规格，须评审合并并调整为对应 MODIFIED delta，不可覆盖。

## Impact

影响版本线：feature/1.0.x、feature/2.0.x、feature/3.0.x。允许依赖差异，禁止业务契约漂移。

硬实现前置：C01, C02, C03。共同规范和测试基线见 [路线图](../../README.md)。

本次文档不改产品代码、POM、CI，不提交或推送。具体修改点、兼容策略、风险见 [design.md](design.md)，验收见 [spec](specs/opencli-remote-collect/spec.md)，任务见 [tasks.md](tasks.md)。
