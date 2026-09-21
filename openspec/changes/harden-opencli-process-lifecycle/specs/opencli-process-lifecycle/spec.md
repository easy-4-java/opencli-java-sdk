# 有界执行、排队超时与进程清理

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-PROC-001 Concurrency ownership is stable
The SDK SHALL keep one stable limiter per client or explicitly shared runtime and SHALL not replace another client's active limiter during construction.

#### Scenario: Clients with different limits
- **WHEN** A 限制为 1 且有任务在跑，此时构造限制为 4 的 B。
- **THEN** A 的在途和后续任务仍受 A 的 1 个许可约束；B 不重置 A。

#### Scenario: Explicit shared runtime
- **WHEN** 两个 Client 显式共享限制为 2 的 Runtime 并并发提交。
- **THEN** 同一许可池中真实在途进程不超过 2，异常与完成路径不泄露许可。

### Requirement: OC-PROC-002 Queueing consumes the total deadline
The SDK SHALL use a monotonic submission-to-execution deadline and SHALL reject queue-expired work before spawning a child.

#### Scenario: Expired in queue
- **WHEN** 唯一许可长期占用，新任务总预算 50ms。
- **THEN** 新任务在预算耗尽后报告 queue timeout；echo fixture 证明该任务从未启动。

#### Scenario: Clock changes
- **WHEN** 等待期间系统墙上时钟回拨或前跳。
- **THEN** 基于单调时间的剩余预算不增加也不提前受墙上时钟误判。

### Requirement: OC-PROC-003 Captured output is finite
The SDK SHALL enforce separately configured stdout/stderr byte budgets, defaulting to 8 MiB and 2 MiB in the proposed API, and SHALL expose overflow as OUTPUT_LIMIT with bounded partial evidence.

#### Scenario: Large stdout
- **WHEN** 子进程生成超过配置 stdout 上限的连续数据。
- **THEN** capture 内存不继续随总输出增长；结果为 OUTPUT_LIMIT、标注截断与字节统计，不返回完整成功。

#### Scenario: Unicode at boundary
- **WHEN** UTF-8 多字节字符恰好跨 capture 截断边界。
- **THEN** 保存的 byte count 正确，文本解码策略明确且不抛出覆盖原始超限的异常；截断状态始终可见。

### Requirement: OC-PROC-004 Cancellation has an owned cleanup path
Timeout, interruption and explicit cancellation SHALL trigger bounded cleanup of only the invocation-owned process resources, restore the caller interrupt flag where applicable, and release permits once terminal handling completes.

#### Scenario: Interrupted waiting thread
- **WHEN** 调用线程等待子进程期间被中断。
- **THEN** 立即进入取消清理，最终释放许可并恢复线程中断标志；不只是等 watchdog，自有进程清理状态有证据。

#### Scenario: Shared daemon remains alive
- **WHEN** 执行超时但系统另有共享 OpenCLI daemon 和用户浏览器进程。
- **THEN** SDK 不按进程名批量终止它们；只处理有明确所有权的进程。

### Requirement: OC-PROC-005 Uncertain cleanup is observable
The SDK SHALL bound cleanup waiting to the configured grace, defaulting to 5 seconds, and SHALL report unknown exit/cleanup state rather than fabricate successful termination.

#### Scenario: Platform cannot confirm a descendant
- **WHEN** Java8 或平台 API 无法确认后代进程已退出。
- **THEN** 保留 UNKNOWN/UNCONFIRMED 清理状态和原失败原因；不得将未知 exit 写为 0。

#### Scenario: Repeated cancel and completion race
- **WHEN** cancel、watchdog 和自然退出几乎同时发生。
- **THEN** 对外只有一个终结结果，许可恰好释放一次；不覆盖已收集的部分输出证据。

### Requirement: OC-PROC-006 Continuous output requires a bounded policy
A typed continuous-output call SHALL require finite capture/duration or a bounded sink policy, and unsupported streaming modes SHALL fail explicitly.

#### Scenario: Unbounded follow requested
- **WHEN** 调用者请求无限 network follow，但选择同步全量 capture 且没有可执行的有限策略。
- **THEN** 在启动前返回不支持的模式错误，而不是无限积累输出。

#### Scenario: Bounded follow
- **WHEN** 调用者设置 duration、输出预算或受控 sink 的有限策略。
- **THEN** 按相应边界结束并区分正常窗口结束、输出超限、取消和 CLI 失败。
