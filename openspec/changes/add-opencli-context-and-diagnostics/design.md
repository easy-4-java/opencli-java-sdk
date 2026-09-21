# C08 Design · 隔离执行上下文与只读诊断

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

上游有profile/window/site-session/keep-tab/trace等上下文；并发使用时不能改全局默认。已有auth/skills/meta可复用，诊断结果也不能把远程跳过当作健康。

证据：E12, E03, U02, U03, U05, U01, U06，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

新增不可变单次上下文、保守只读诊断与可选sitemap提示集成。完整sitemap文件管理、自动修复和Java pipeline执行均暂缓。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

上下文优先级为显式单次值→Client默认值→CLI默认值；“未提供”与false/空值分别建模，执行时生成快照，不改System全局环境或profile use。

### D2

profile传递使用真实root/global option契约；window/site-session/keep-tab遵守相应namespace/command能力，不将所有参数盲目加到每条命令。

### D3

diagnose返回每项状态/来源/时刻/范围；版本、可执行文件、Help和transport能力分别记录。remote-only环境的local probe为NOT_APPLICABLE或UNKNOWN，不是HEALTHY。

### D4

只读诊断不得登录、auth refresh、daemon restart、profile use、plugin install/update。若需调用状态命令，先核实当前版本schema和副作用边界。

### D5

sitemap字段只做optional metadata，typed细节必须先采样；不自动读写overlay。pipeline在JS模块边界，另立ADR证明执行路径后才排实施，不制造opencli pipeline命令。

## Alternatives considered

不使用临时修改全局profile再改回的方案，因为并发和失败会污染状态。不把有提示等同于授权读文件，不在诊断失败时自动进行有副作用修复。

## Risks / Trade-offs

某些doctor/status实现可能包含自修复行为，不能仅凭命令名宣布只读。实现前审计实际版本并优先选择不变更状态的probe；不能证明时报告未支持的只读诊断。

## Migration plan

保持原meta/auth/skills/external入口。旧global properties仍可用于默认配置；推荐不可变单次context，明确deprecated与兼容策略，不新增同义Tools平台。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-CTX-001, OC-CTX-002, OC-CTX-003, OC-CTX-004, OC-CTX-005, OC-CTX-006。

对应 [spec](specs/opencli-context-diagnostics/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
