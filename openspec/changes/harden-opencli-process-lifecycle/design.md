# C02 Design · 有界执行、排队超时与进程清理

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

静态 Semaphore 会被新 Client 替换；排队无超时，stdout/stderr 无界，中断路径缺少明确清理。现有 watchdog 并不能证明全部资源有界。

证据：E01, E04, E12，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

限制单次执行、队列和 capture；定义 cancel/timeout 的所有权和清理结果。保留同步 API；不实现持久化工作流或替换共享 OpenCLI daemon。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

每 Client 或显式共享 Runtime 使用稳定的许可管理器。默认并发沿用 max(2, availableProcessors)，0 表示默认，负值拒绝；不再把 0 文档化为无限。

### D2

单个 command deadline 从提交开始用单调时钟计量，覆盖排队与执行；许可等待使用剩余预算。cleanup 使用独立最多 5 秒 grace，报告实际清理状态。

### D3

新设计默认 stdout 8 MiB、stderr 2 MiB；任一超限结束为 OUTPUT_LIMIT，有限部分结果和 byte counts 保留。预算值允许显式配置，但必须正数且校验溢出。

### D4

超时、取消、中断、超限走同一终结路径；只清理本次拥有的子进程/后代，许可最终释放。Java8/不同平台对子孙进程的能力限制必须用测试报告表达。

### D5

同步完整采集模式不承诺无限 follow。需要连续输出时明确有限 duration/output budget 或受控 sink；不能悄悄吞掉超限后的数据并返回完整成功。

## Alternatives considered

不采用每次 new Semaphore 或只增加线程池大小的方案；也不采用“遇到超时 kill 全部 opencli/Chrome”作为清理手段。

## Risks / Trade-offs

杀进程与读取流存在竞态；输入 deadline 与 cleanup 时间不同，调用时长可能达到 deadline + cleanup grace。必须保留 outcome 与 cleanupState，不能因 cleanup 未证实而宣称进程已终止。

## Migration plan

对旧静态配置 API 做兼容封装/弃用说明，新 Client 不再隐式重设其它 Client。保留现有异常父类，增量加入 queue timeout、output limit 等可区分原因。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-PROC-001, OC-PROC-002, OC-PROC-003, OC-PROC-004, OC-PROC-005, OC-PROC-006。

对应 [spec](specs/opencli-process-lifecycle/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
