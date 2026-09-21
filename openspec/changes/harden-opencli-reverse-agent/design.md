# C05 Design · 反向 Worker 状态机、背压与去重

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

现有反向 WebSocket Client 已实现接单执行，但 cached pool 无界、注册 gate 和去重不足、关闭不等待任务。必须补足 Worker 边界，而不是假设它已是应用端 WS RPC。

证据：E07, E01, E04，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

维持 register/registered/collect/result/ping/pong 协议，增加本地状态机、有限任务治理、代际和去重。不给服务器强加新重放协议，不保证跨重启 exactly-once。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

状态 NEW→CONNECTING→REGISTERING→READY→DRAINING→CLOSED；意外断线可在非 DRAINING/CLOSED 下转 CONNECTING。READY 前不运行 collect；CLOSED 终态，close 幂等。

### D2

拟定默认 workerConcurrency=4、pendingQueueCapacity=32；正数且有限、可配置。过载使用既有 result success=false/error 文本表示 BUSY，不增加 wire enum 作为既定服务端契约。

### D3

request_id 非空、UTF-8 最多128字节；按中心身份+agent实例生命周期+ID建立去重表。完成缓存拟定10分钟/1024条；不驱逐在途记录以制造重放窗口。

### D4

同 ID 不同 payload digest 拒绝。同 ID 同 payload 在途不再次执行，完成结果可在授权同中心连接上返回。进程重启不保证缓存，保留至少一次投递风险说明。

### D5

任务绑定原连接代际，迟到完成不能隐式投递到新连接；新代际相同请求 ID 的合法重复可读缓存。stop 停接单并进入有界 drain；默认最多5秒后取消自有在途任务。

## Alternatives considered

不采用仅依靠进程 Semaphore 来限制 Worker 线程的方案，也不采用断线后无条件重试所有任务。消息接收线程继续只做有限解析/准入，不执行长时间 CLI。

## Risks / Trade-offs

结果发出失败不说明执行失败；重复消息与连接代际共同处理才能减少副作用重复。去重是进程内的有界保证，不能将其描述为分布式 exactly-once。

## Migration plan

保留现有消息类型和兼容字段；对空 ID、未注册接单和 start-after-close 由隐式容忍改为明确失败。需要重新启动已关闭实例时创建新实例，不复用已 shutdown pool。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-WS-001, OC-WS-002, OC-WS-003, OC-WS-004, OC-WS-005, OC-WS-006。

对应 [spec](specs/opencli-reverse-agent/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
