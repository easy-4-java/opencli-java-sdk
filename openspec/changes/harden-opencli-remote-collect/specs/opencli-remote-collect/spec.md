# Legacy HTTP collect 边界与保真

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-REMOTE-001 Transport scope is explicit
The SDK SHALL distinguish the opencli-admin legacy collect contract from upstream OpenCLI CLI capabilities and SHALL report the supported command/request subset before dispatch.

#### Scenario: Browser session in legacy mode
- **WHEN** REMOTE_AGENT_HTTP 收到 ["browser", "session-a", "state"]。
- **THEN** 未建立服务端 Browser 契约时在发 HTTP 前拒绝，不把 session-a 当 command。

#### Scenario: Supported adapter request
- **WHEN** 调用者提交已验证的单 adapter/command 结构化 collect 请求。
- **THEN** 按现有 /collect DTO 发送，行为与锁定 server fixture 一致；不新增自定义 wire 字段。

### Requirement: OC-REMOTE-002 Lossy conversions never dispatch
A raw-to-collect conversion SHALL preserve representable values and SHALL reject ambiguous or lossy forms before network I/O rather than silently normalizing them.

#### Scenario: Repeated option or terminator
- **WHEN** raw argv 含两次 --tag 或独立 --，旧 Map 协议无法保留其语义。
- **THEN** 返回明确 UNSUPPORTED_CAPABILITY/转换失败，并断言 fixture server 的请求数为零。

#### Scenario: Negative and empty values
- **WHEN** 输入含 --limit -1、空字符串或前后空格 value。
- **THEN** 只在已验证 schema/DTO 可保真时发送；否则明确拒绝，绝不把 -1 转成 flag 或丢空值。

### Requirement: OC-REMOTE-003 Remote evidence is not a fabricated process exit
The SDK SHALL separately retain HTTP status, legacy success, bounded result payload and exit provenance; compatibility exit 0/1 SHALL be labeled synthetic.

#### Scenario: HTTP 200 business failure
- **WHEN** 服务端返回 HTTP 200、success=false、error 文本。
- **THEN** 结果是业务失败；HTTP200 仍保留，compatibility exit 可为1但 observedRemoteExit 为未知。

#### Scenario: HTTP failure
- **WHEN** 服务端返回 503 与任意 HTML 或 JSON body。
- **THEN** 归类传输/HTTP失败，保存安全状态和有界受控证据；不得把503当 CLI 自己的退出码。

### Requirement: OC-REMOTE-004 Malformed responses are explicit failures
The SDK SHALL validate the legacy envelope shape and SHALL not turn malformed, null or missing-required-status responses into an empty successful result.

#### Scenario: Null envelope
- **WHEN** HTTP200 body 为 null、空文本或数组而非声明 envelope。
- **THEN** 返回 PROTOCOL_FAILURE，而不是成功的 []。

#### Scenario: Absent items on successful legacy reply
- **WHEN** 经过验证的 legacy server 契约允许 success=true 且 items 缺失。
- **THEN** 按该契约映射空结果，并标注字段缺省来源；不得推广为任意 malformed 响应都可忽略。

### Requirement: OC-REMOTE-005 Remote budgets and uncertainty are bounded
The SDK SHALL apply the remaining execution deadline and finite body limits to remote I/O, and SHALL not automatically replay outcome-unknown requests.

#### Scenario: Timeout after dispatch
- **WHEN** 服务端接收请求后连接超时，SDK 不知道命令是否完成。
- **THEN** 返回 timeout/transport 状态并标明远端 outcome unknown；请求不会自动重发。

#### Scenario: Oversized body
- **WHEN** 响应体超过配置的有限预算。
- **THEN** 在有界资源内中止读取，标记响应超限/不完整，不返回未完整验证的成功结果。

### Requirement: OC-REMOTE-006 Credentials use an explicit secure boundary
The SDK SHALL keep configured credentials out of URLs and default diagnostics, maintain TLS verification, and use authentication only according to an explicitly supported server contract.

#### Scenario: Authenticated fixture
- **WHEN** 调用者提供 fixture server 支持的 header 凭据。
- **THEN** 凭据仅用于相应请求，不出现 URL、异常 message 或默认日志。

#### Scenario: Unsupported authentication assumption
- **WHEN** 能力探测未证明服务端支持某种 OAuth/签名协议。
- **THEN** SDK 不宣称该协议可用，也不自动生成或发送未经配置的认证数据。
