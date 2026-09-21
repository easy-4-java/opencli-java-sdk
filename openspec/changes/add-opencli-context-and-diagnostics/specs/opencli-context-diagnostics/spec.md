# 隔离执行上下文与只读诊断

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-CTX-001 Invocation context is isolated
The SDK SHALL snapshot per-invocation context and SHALL apply explicit invocation values before client defaults without modifying other clients, global environment or CLI default profiles.

#### Scenario: Concurrent profiles
- **WHEN** 调用A选择profile-a/window=background，调用B选择profile-b/window=foreground。
- **THEN** 各自argv/env只包含自己的上下文，A失败不会改变B或CLI全局profile。

#### Scenario: Absent versus false
- **WHEN** Client默认keepTab=true，单次明确设置false；另一调用未提供值。
- **THEN** 前者使用false，后者继承true，不能因为false被当作空值而丢失覆盖。

### Requirement: OC-CTX-002 Context flags respect capability scope
Context construction SHALL honor root, namespace and leaf option scopes and SHALL reject unsupported typed combinations before dispatch.

#### Scenario: Unsupported option scope
- **WHEN** 某非Browser命令schema没有site-session能力，typed context却指定它。
- **THEN** 在启动前明确拒绝该组合；只有调用者移除该字段后才能提交，不能无声忽略或注入无效flag。

#### Scenario: Profile is supplied
- **WHEN** 当前本地CLI支持global profile，调用者指定profile-a。
- **THEN** 通过已验证global参数传递，不通过profile use修改全局设置。

### Requirement: OC-CTX-003 Diagnostics report evidence rather than optimism
Diagnostics SHALL return per-check status, observation time, source and applicability and SHALL not equate skipped, stale or unreachable checks with healthy runtime.

#### Scenario: Remote-only execution
- **WHEN** 仅配置legacy HTTP执行，无法检查用户本地Chrome/daemon。
- **THEN** 相应check标记NOT_APPLICABLE/UNKNOWN；不报告本地浏览器健康。

#### Scenario: Version works but schema fails
- **WHEN** --version可执行，但structured help失败。
- **THEN** 分别报告executable可用与schema未知/失败，不给统一“全部兼容”结论。

### Requirement: OC-CTX-004 Default diagnostics do not repair
The diagnostic path SHALL be read-only according to verified command behavior and SHALL not trigger login, refresh, restart, install, update or profile mutation.

#### Scenario: Auth expired
- **WHEN** 诊断发现认证已过期。
- **THEN** 返回认证状态与安全说明，不执行auth refresh/login。

#### Scenario: Unverified doctor side effects
- **WHEN** 当前doctor实现的只读性尚未被验证。
- **THEN** 使用已证实的安全probe或报告该项未验证，不以名字假定只读后直接执行。

### Requirement: OC-CTX-005 Sitemap hints remain optional untrusted data
The SDK SHALL retain optional sitemap hints with provenance, SHALL keep absent metadata distinct from false, and SHALL not automatically read or write site-memory files.

#### Scenario: Hint available
- **WHEN** open/analyze fixture提供sitemap hint。
- **THEN** 返回给调用者并标明来源，但不执行其中workflow或访问任意本地目录。

#### Scenario: Missing fixture contract
- **WHEN** 具体sitemap字段的当前版本fixture尚未取得。
- **THEN** 只保留raw/extension metadata，typed字段契约保持待验证，不从旧说明猜required字段。

### Requirement: OC-CTX-006 Unestablished pipeline interfaces remain deferred
The SDK SHALL not advertise a public pipeline CLI or single-process Java batch guarantee based solely on the upstream JavaScript package export.

#### Scenario: Package export discovered
- **WHEN** package.json导出./pipeline，但没有已验证的公共CLI/server调用契约。
- **THEN** 能力矩阵列为JS模块/DEFERRED；不生成opencli pipeline调用方法。

#### Scenario: Future pipeline proposal
- **WHEN** 后续需要Java批处理或Node bridge。
- **THEN** 必须提交独立ADR/Change，明确进程模型、兼容性、安全和真实性能证据后再实施。
