# C04 Design · Legacy HTTP collect 边界与保真

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

argv 转 site/command/Map 的旧协议会丢失重复选项、空值、负数等信息，也无法默认表达 Browser session 或任意 root 命令。当前 HTTP 返回的 0/1 是映射值，不是已观察的远端退出码。

证据：E05, E06, U03，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

明确 opencli-admin legacy collect 子集和响应语义；小范围隔离 transport 能力。保留已有 wire DTO，不新建未获服务端支持的 endpoint 或 WS RPC。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

声明 LOCAL_PROCESS 的显式 argv 能力，与 ADAPTER_COLLECT_V1 的已验证结构化子集分开。未验证的命令类别为 UNKNOWN 或 UNSUPPORTED，不能默认 fallback。

### D2

raw→legacy 必须先证明可无损表达，否则在网络发送前失败；尤其 Browser session、单词 root 命令、external passthrough、重复 option、-- terminator 不得启发式误路由。

### D3

需要原始 argv transport 时另立 server 协同变更；本 Change 不添加 /execute、stdin、stream 或取消 wire 字段。

### D4

HTTP status、success/items/error、SDK错误原因、synthetic exit 分层保存。旧 0/1 可作兼容字段，但 observedRemoteExit 保持未知。

### D5

请求超时按剩余总预算；响应体有限。header/auth 仅做显式配置与实际服务端契约校验，不把 token 放 URL、不关闭 TLS 验证。

## Alternatives considered

不继续依靠两 token + Map 解析猜测全部 CLI；也不在无 server 资料时自创 v2 wire。先保留已可表达的稳定子集，显式拒绝不支持项。

## Risks / Trade-offs

此前误发送的调用将更早失败，这是必要行为收紧。HTTP 超时可能发生在远端已执行之后，因此 outcome 必须允许 unknown，不能自动重试写命令。

## Migration plan

保留 collect(request) 入口和 DTO；修复 invoke→legacy 的准入检查，给不受支持的旧调用提供本地运行或未来显式 v2 迁移说明。服务端变更不属于本包授权范围。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-REMOTE-001, OC-REMOTE-002, OC-REMOTE-003, OC-REMOTE-004, OC-REMOTE-005, OC-REMOTE-006。

对应 [spec](specs/opencli-remote-collect/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
