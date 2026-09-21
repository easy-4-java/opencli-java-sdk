# 反向 Worker 状态机、背压与去重

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-WS-001 Registration gates command execution
The reverse worker SHALL execute collect only in READY after a valid registration acknowledgement and SHALL remain a worker role rather than claim an outbound RPC client contract.

#### Scenario: Early collect
- **WHEN** 连接建立后尚未收到 registered 就收到 collect。
- **THEN** 不提交 CLI任务；按现有错误回复或安全断开策略处理，并且本地执行次数为零。

#### Scenario: Ready collect
- **WHEN** 当前代际已经注册成功，收到合法任务。
- **THEN** 在容量和权限检查后执行本地 CLI，按既有 result 消息回复。

### Requirement: OC-WS-002 Worker concurrency and queue are finite
The worker SHALL enforce explicit finite concurrency and pending-queue capacity, proposed defaults 4 and 32, and SHALL reject overload without spawning extra waiting threads.

#### Scenario: Queue full
- **WHEN** 已有4个执行任务和32个等待任务，再收到一个 collect。
- **THEN** 返回可识别的忙碌失败，不为该请求创建额外等待线程或子进程。

#### Scenario: Invalid capacity
- **WHEN** 配置并发或队列为负数、无界值或溢出。
- **THEN** 在启动连接前拒绝配置；不悄悄转为 cached/unbounded 模式。

### Requirement: OC-WS-003 Request identity is required and bounded
The worker SHALL require a nonblank request_id no longer than 128 UTF-8 bytes and SHALL deduplicate identical IDs within the same configured and verified center identity and agent-instance lifetime.

#### Scenario: Duplicate in flight
- **WHEN** 同一作用域连续收到两个相同 request_id 与 payload。
- **THEN** 真实 CLI 执行一次；重复请求不能创建第二个任务。

#### Scenario: ID collision with changed payload
- **WHEN** 相同 request_id 的第二条消息携带不同命令或参数。
- **THEN** 明确冲突失败，原任务不被替换，第二个载荷不执行。

### Requirement: OC-WS-004 Deduplication retention is bounded and honest
The worker SHALL bound completed-result retention by TTL and capacity, proposed defaults 10 minutes and 1024 entries, without evicting live in-flight identity protection or claiming restart-safe exactly-once.

#### Scenario: Completed duplicate
- **WHEN** 合法重复在完成缓存有效期内到达。
- **THEN** 返回保留结果而不重新执行，结果来源明确为缓存。

#### Scenario: Restart or expiration
- **WHEN** agent 进程重启或完成记录已过期。
- **THEN** 不声称此前执行可被去重；暴露保证边界，不能用缓存缺失判断此前未执行。

### Requirement: OC-WS-005 Connection generations isolate late results
The worker SHALL associate execution with its accepted connection generation and SHALL not implicitly send old-generation completion through an unrelated new connection.

#### Scenario: Disconnect during write
- **WHEN** 写任务在 generation1 执行，连接断开并进入 generation2。
- **THEN** 任务不会因重连自动再执行，迟到结果不会无条件发送到 generation2。

#### Scenario: Explicit duplicate on new generation
- **WHEN** 同一合法中心在新代际重发已完成且仍在缓存中的 ID/payload。
- **THEN** 通过去重和中心身份校验后可返回缓存结果；不把它视为任意连接可领取的数据。

### Requirement: OC-WS-006 Close is terminal and bounded
The worker SHALL stop admission, drain or cancel owned tasks within a finite close budget, release resources, and reject start after CLOSED.

#### Scenario: Close with pending work
- **WHEN** close 时有运行和排队任务。
- **THEN** 立即停止接单，按 drain/cancel 策略在配置预算内处理；不让排队任务在关闭后自行新启动。

#### Scenario: Repeated close and restart
- **WHEN** 连续调用 close 两次，然后 start。
- **THEN** close 幂等且不抛出 pool 竞态；start 明确拒绝已关闭实例，必须创建新实例。
