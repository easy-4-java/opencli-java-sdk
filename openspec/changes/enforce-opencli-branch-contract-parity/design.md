# C10 Design · 共享契约测试、分支差异与上游锁定

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

原始审计快照中，1.x有五个coverage类，2.x/3.x对应目录未出现；提交前远端2.x/3.x已补回套件，当前3.x metadata为1277命令/173站点，来源已不再是作者绝对路径。见 [提交前复核补记](../../../docs/architecture/opencli-java-sdk-publish-check.md)。后续复用现有套件并核对其行为，不重复恢复、不删除现有测试；仍需完整来源锁定、参数语义及真实执行证据。

证据：E10, E11, E12, E13, E14, E15, E16, E17, E18，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

建立共享contract vectors、上游来源锁定、三线允许差异和真实CI验收；修正README/POM事实。分基础阶段和最终集成门禁，不实施站点业务能力。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

以规范/fixture为共同事实；提议feature/2.0.x先实现行为修复，再分别移植1.x和3.x。规范草案不代表该分支策略已获用户正式批准。

### D2

允许Java8/17/21、Jackson2/3、Maven3/4及必要OS适配差异，禁止argv/error/deadline/remote准入/privacy语义漂移。不是整POM或整分支merge。

### D3

保留1275/173为historicalfixture metadata；新snapshot必须有来源version/ref/hash/采集命令/时刻/范围。每次upstream升级形成差异报告，不凭计数相等判断兼容。

### D4

目录枚举、完整argv、协议fixture、typeddecoder、live运行五层分别报告；未跑的维度为NOT_RUN。Recording测试不能证明真实CLI行为。

### D5

CI必须记录实际HEAD、tool版本、testcount、skipcount、exit与artifact；3.x使用它自己的wrapper。未触发、0测试和文档检查不等于SDK通过。

### D6

C10基础（锁定/fixture runner/门禁框架）先做；C10最终关闭需选定范围C01–C09全部完成。该关闭条件不是C01–C09实现前的循环依赖。

## Alternatives considered

不以codecoverage行数或manifest数量代替契约测试；不选择盲目将3.x源码拷回Java8；不把未触发CI解释为通过。

## Risks / Trade-offs

依赖和工具版本在环境中可能不可获取，必须记录真实失败而不绕过门禁。Jacoco阈值、发布配置与行为测试不同，不能只改阈值制造通过。

## Migration plan

在已补回的coverage基础上增量强化契约与适配测试，修复仍存在的README工具声明和示例。规范只有完成评审、实现和真实验收后才archive到openspec/specs；本包全部implementation tasks保持unchecked。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-PARITY-001, OC-PARITY-002, OC-PARITY-003, OC-PARITY-004, OC-PARITY-005, OC-PARITY-006。

对应 [spec](specs/opencli-branch-contract-parity/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
