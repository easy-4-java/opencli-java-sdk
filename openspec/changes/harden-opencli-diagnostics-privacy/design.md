# C09 Design · 默认日志与异常的隐私边界

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

当前异常与日志会打印commandLine、参数摘要、HTTP body preview和远端error，可能泄露提示词、代码和凭据。上游抓包脱敏不解决Java日志问题。

证据：E01, E02, E06, E07, E09，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

修复SDK默认诊断投影、异常公开文本与显式导出策略，保留已授权调用者的raw业务数据。不是内容审查，也不无声删改执行结果。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

默认采用元数据允许表：事件类型、correlationId、结构性command类别、耗时、字节数、状态码。普通业务value即使键名不敏感也不记录。

### D2

异常默认message采用安全摘要，不拼commandLine、body、URLquery或远端自由文本。错误code可保留，help/message原文放受控error结果，不进入普通日志。

### D3

provider异常的cause/toString可能携带request/body；不能只脱敏顶层message而把完整cause交给logger。用安全cause描述或明确raw受控字段。

### D4

raw stdout/stderr和可选remoteRawHttpBody属于调用者已授权结果；rawHTTPcapture保持默认关闭。显式导出需独立opt-in、有限大小、路径/凭据清洗，不改变执行语义。

### D5

trace/sitemap路径视为数据；默认不跟随，不自动上传。对canary secret做端到端输出负例，覆盖失败、timeout、parse、WS和HTTP。

## Alternatives considered

不采用“只替换password/token key”的粗糙正则作为唯一保证，因为参数和错误自由文本没有固定键。不采用删掉raw结果的方案，因为会破坏调用者业务读取。

## Risks / Trade-offs

调用者主动打印raw仍可能泄露，SDK不能保证外部日志系统。保证范围是SDK控制的默认logs、默认异常文本与显式diagnostic export；文档必须写清。

## Migration plan

先减少默认payload日志，保留结构性定位信息。对于依赖异常原文的应用，提供受控error/result访问并迁移；不默认开启debug export来复现老泄露行为。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-PRIV-001, OC-PRIV-002, OC-PRIV-003, OC-PRIV-004, OC-PRIV-005, OC-PRIV-006。

对应 [spec](specs/opencli-diagnostics-privacy/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
