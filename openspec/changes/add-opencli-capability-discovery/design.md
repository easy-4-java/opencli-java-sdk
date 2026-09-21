# C06 Design · 结构化能力发现与可追溯快照

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

已有通用 Adapter 入口，但缺少基于实际上游 Help 的参数契约与可追溯快照。不能继续把手写常量和未标版本的1275条fixture当成动态全集。

证据：E02, E10, U02, U03, U05，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

新增 capability service、schema模型与受限发现缓存，复用 adapter(id)。不自动生成全部返回DTO，不验证每个网站账号，不安装或运行插件业务命令。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

直接消费 Structured Help 的 command_options/namespace_options/global_options、positionals、required/takes_value/negate/choices 等真实字段。保留 raw 与未知字段。

### D2

browser <session> <command> 的占位段只进入 schema 路径模型，绑定真实会话后才能构造 argv。不得将 <session> 字面量或 dottedname 自动当成 CLI token。

### D3

快照记录 executable identity、packageVersion、可用commit、help/manifest hash、采集时刻、配置指纹、命名空间与观察范围。不存在的 provenance 设为 unknown，不伪造。

### D4

定义 AVAILABLE/STALE/UNKNOWN/UNSUPPORTED；缺少帮助或旧格式不是空命令列表成功。runtime overrides 和插件只能在实际发现范围内报告来源。

### D5

发现仅运行信任且已配置的 CLI 的 version/help/描述操作，并使用有限预算。raw本地escape hatch保留，strict typed构造可依赖有效schema；remote仍受C04限制。

## Alternatives considered

不新增与 adapter(id) 同义的动态 facade，不依据旧 manifest 宣称全量执行覆盖。Help-only 的代码生成只适用于输入描述，领域输出模型另由 C07负责。

## Risks / Trade-offs

同一版本号下本地 override 也会改变能力；只用version做cachekey不够。Help执行可能加载插件模块，因此必须使用信任可执行文件/隔离采集环境，不能把“help”称作完全无代码执行。

## Migration plan

新增服务为显式调用，不使每个旧调用都触发联网发现。原有 AdapterIds 常量仍可用但文档改称快照；schema缺失不禁止用户明确选择本地raw。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-CAP-001, OC-CAP-002, OC-CAP-003, OC-CAP-004, OC-CAP-005, OC-CAP-006。

对应 [spec](specs/opencli-capability-discovery/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
