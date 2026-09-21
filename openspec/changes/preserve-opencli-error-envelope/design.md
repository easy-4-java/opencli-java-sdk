# C03 Design · 上游错误协议解析与证据保真

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

当前 SDK 主要识别 JSON 外形；上游 Adapter 错误使用 stderr YAML，wire 字段为 help，可附 trace。只解析 stdout JSON 将漏掉最重要的失败信息。

证据：E01, E08, E09, U04, U05，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

增加错误 decoder、错误来源与 partial-result 保真；兼容 JSON 和纯文本。不得把所有 stderr 输出都视作错误，也不自动重试命令。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

当前优先解析失败调用的 stderr YAML envelope；同时接受合法 JSON envelope。识别 ok=false 与必要 error 字段，普通日志回退文本。

### D2

wire 原名为 help；可提供 Java 侧兼容 alias，但原始字段与扩展信息保留。processExitCode 与 envelope.exitCode 不一致时两者都记录。

### D3

解析预算提出为 256 KiB、最大嵌套深度 32、单个文档、禁用自定义标签与别名。超过限制标记 unparsed/limit，不覆盖先前业务或进程失败。

### D4

traceId/dir/summaryPath/receiptPath/status 仅作为返回元数据；不自动读取 trace 指向的文件。decoder 本身无重试或安装副作用。

### D5

保留现有异常兼容父类；区分 COMMAND_FAILURE、SDK_TIMEOUT、CANCELLED、TRANSPORT_FAILURE 和 PROTOCOL_FAILURE。

## Alternatives considered

不采用“所有命令附加 -f json 后只读 stdout”的方案，因为 U05 的错误渲染独立输出 YAML；不按 exit=1 一律 UNKNOWN。

## Risks / Trade-offs

新增 YAML parser 有依赖/安全成本，需三分支兼容验证。某些命令可能输出非 envelope YAML；shape 校验必须保守。纯文本和 malformed 输出仍需保留可诊断证据。

## Migration plan

旧 raw/lenient 入口继续存在；strict error decoder 不修改原始已授权输出，也不把无法解析的失败变成成功。错误对象追加字段，不删除旧 partial result。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-ERR-001, OC-ERR-002, OC-ERR-003, OC-ERR-004, OC-ERR-005, OC-ERR-006。

对应 [spec](specs/opencli-error-envelope/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
