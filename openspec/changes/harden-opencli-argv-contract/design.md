# C01 Design · 参数保真与统一调用契约

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

三个分支的 Executor 会 trim 并丢弃空白参数，AdapterChannel 也有同类处理。已有通用 Adapter API，应修复它而非重复建设。

证据：E01, E02, E05, E17, E18，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

修改参数构造和校验；保留 List、varargs、结构化请求及现有 Client 门面；不新增 shell 执行模式，不扩展站点功能。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

把 command/executable/adapter 的标识符校验与参数值处理分开。合法 value 原样传递；null 显式失败；已知必填标识符拒绝空白。

### D2

统一经过不可变 token 快照；不得 trim、过滤空字符串、拆分包含空格的 value 或把整条命令拼成 shell 字符串。

### D3

结构化入口增加有序 option occurrence 语义；旧 Map 接口继续用于单值子集。flag、带值布尔、negated flag 分开表示；无 schema 时不猜测。

### D4

本地不解释 -- 后内容；Windows shims 的限制单独声明和验证。不能用 POSIX 字符串拼接测试声称所有平台已覆盖。

## Alternatives considered

拒绝“保留 trim 只新增另一套 raw API”的方案：现有 API 仍会静默破坏业务值。采用兼容入口内部保真，记录此前依赖 trim 的调用者需要显式处理。

## Risks / Trade-offs

一部分调用者可能依赖历史清理行为。迁移说明明确行为修复；不提供默认重新丢值的兼容模式。标识符中是否允许空格以真实 CLI 契约验证，不借此修改数据参数。

## Migration plan

先新增真实 argv echo 测试并观察失败，再统一 helpers 与入口；旧方法签名保留，旧 null 容忍改为可定位的参数异常。跨分支分别编译，不改整份 POM。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-ARGV-001, OC-ARGV-002, OC-ARGV-003, OC-ARGV-004, OC-ARGV-005。

对应 [spec](specs/opencli-argv-contract/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
