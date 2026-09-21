# 结构化能力发现与可追溯快照

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-CAP-001 Discovery follows the real structured-help shape
The capability service SHALL model commands, positionals, command_options, namespace_options and global_options from the locked upstream schema and SHALL retain unknown fields.

#### Scenario: Nested browser namespace
- **WHEN** fixture 包含 tab/get 子命令、全局 profile 和命名空间 options。
- **THEN** 层级和作用域被保留，叶子相同名的不同scope字段不被错误合并。

#### Scenario: Future extra field
- **WHEN** 合法 Help 新增未识别字段但基本结构有效。
- **THEN** 可用核心结构正常解析，额外字段可追溯；不丢弃raw也不伪造其含义。

### Requirement: OC-CAP-002 Value-taking and negation are explicit
Schema-aware argument validation SHALL distinguish mandatory options, required/optional values, choices, variadic positionals, flag negation and absent versus explicit false.

#### Scenario: Optional value omitted
- **WHEN** 某 option 声明 takes_value=optional，调用者只给 presence。
- **THEN** 按对应schema构造bare flag，不强行消费后一个业务 positional。

#### Scenario: Invalid choice
- **WHEN** 调用者为已声明 choices 的 typed option提供未允许值。
- **THEN** 启动前返回字段级校验错误；不擅自改写为默认值。

### Requirement: OC-CAP-003 Session placeholders are bound, not executed
The SDK SHALL preserve namespace path placeholders in schema metadata and SHALL require explicit binding before typed execution.

#### Scenario: Browser state path
- **WHEN** schema command 为 opencli browser <session> state，调用者绑定 session-a。
- **THEN** 最终argv使用 browser/session-a/state，不传字面<session>。

#### Scenario: Missing session binding
- **WHEN** typed调用未提供所需session。
- **THEN** 明确校验失败，不生成空session，也不使用其它并行任务的默认session。

### Requirement: OC-CAP-004 Snapshots carry provenance and scope
Each capability snapshot SHALL identify its executable/configuration scope, observed version, timestamps and content hashes, and SHALL not treat bundled manifests as all runtime extensions.

#### Scenario: Unresolved local override
- **WHEN** 无法确认同名命令来自bundled还是local override。
- **THEN** 来源标为UNKNOWN或unresolved，不以文件名推断为上游已验证。

#### Scenario: Same version changed installation
- **WHEN** 同一packageVersion下help内容或配置指纹改变。
- **THEN** 快照失效或更新并生成新hash；不继续当作相同能力事实。

### Requirement: OC-CAP-005 Unavailable discovery is not an empty success
The service SHALL report UNKNOWN, STALE or UNSUPPORTED with reasons when appropriate, and SHALL keep an explicitly selected local raw invocation available without fabricating schema support.

#### Scenario: Old CLI plain-text help
- **WHEN** CLI返回文本帮助而不是当前结构化schema。
- **THEN** 状态为UNKNOWN/unsupported-format，不能解析成0命令的AVAILABLE。

#### Scenario: Cached stale data
- **WHEN** 刷新失败但有旧快照。
- **THEN** 返回旧快照时标明STALE和采集时刻；不得提升为当前CLI兼容认证。

### Requirement: OC-CAP-006 Discovery is bounded and does not run business operations
Discovery SHALL use only configured trusted executables and bounded descriptive probes; it SHALL not install plugins, log in, refresh credentials or execute discovered command examples.

#### Scenario: Malicious description
- **WHEN** help内容包含要求执行安装/删除/外发的命令示例。
- **THEN** 作为不可信描述数据保留，不执行它，不把它当作SDK指令。

#### Scenario: Slow help
- **WHEN** help在deadline内不返回或输出超预算。
- **THEN** 按C02终止/限流，状态为发现失败；不无限等待或反复重试。
