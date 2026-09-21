# 上游错误协议解析与证据保真

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-ERR-001 Current YAML error output is supported
The SDK SHALL decode validated OpenCLI failure envelopes from stderr YAML independently of the success output format, including trailing YAML comments.

#### Scenario: JSON success format with YAML failure
- **WHEN** 成功格式为 -f json，进程非零，stderr 含 ok:false / error.code:AUTH_REQUIRED / error.help 与 # AutoFix 注释。
- **THEN** 解析出错误字段并保留 stderr；不得因为 stdout 为空而丢失错误或转为成功。

#### Scenario: Plain diagnostics
- **WHEN** stderr 只有警告文本，没有合法 envelope，进程正常退出。
- **THEN** 不根据文本关键词虚构 CLI 业务错误；结果遵从已观察的执行状态。

### Requirement: OC-ERR-002 Wire fields and trace remain available
A decoded error SHALL preserve code, message, help, exitCode, optional cause/stack and trace metadata; unknown error codes and fields SHALL remain accessible.

#### Scenario: Future plugin error
- **WHEN** 合法 envelope 的 code 是尚未建模的新值，并带扩展字段及 trace。
- **THEN** 原 code、help 和扩展信息可访问；可归类为通用 CommandFailure，但不得改写原 code 为 UNKNOWN。

#### Scenario: Trace points to a file
- **WHEN** envelope 包含 dir 与 summaryPath。
- **THEN** 只返回元数据；解析错误不访问该路径，不自动上传或打开文件。

### Requirement: OC-ERR-003 Exit origins are separate
The SDK SHALL keep the observed process exit, reported envelope exit and any synthetic compatibility exit as separate values with provenance.

#### Scenario: Exit mismatch
- **WHEN** 真实进程退出码为 1，合法 envelope.error.exitCode 为 77。
- **THEN** 同时保存 1 和 77、各自来源与 mismatch 标记；不覆写一个来匹配另一个。

#### Scenario: Unobserved exit
- **WHEN** SDK 取消导致尚未观察到退出值。
- **THEN** processExitCode 保持未知；不会把 cancellation 推断成 CLI 自己返回 75 或 130。

### Requirement: OC-ERR-004 Parsing is safe and bounded
Envelope parsing SHALL enforce a proposed 256 KiB input budget, maximum nesting depth 32, a single document and no aliases or custom YAML tags, without losing the original execution failure.

#### Scenario: Malicious YAML
- **WHEN** stderr 含自定义对象标签、别名扩张、多文档或深度超限结构。
- **THEN** decoder 停止并标记无法安全解析；不构造任意对象、不执行代码，原失败与有限 raw 仍可访问。

#### Scenario: Malformed JSON
- **WHEN** 错误输出以 { 开头但无法解析，进程已经非零。
- **THEN** 结果仍是失败并标注 parser 状态，不因为 fallback 为 text 而变成 typed success。

### Requirement: OC-ERR-005 SDK and transport causes stay distinct
Error handling SHALL distinguish CLI business failure from SDK resource limits and transport/protocol failures, and SHALL not let decoder errors mask the primary outcome.

#### Scenario: Timeout with partial envelope
- **WHEN** SDK deadline 到期，只收到部分 YAML。
- **THEN** 主结果为 SDK_TIMEOUT，保留部分输出和解析不完整状态，不伪造完整 CLI TIMEOUT。

#### Scenario: Network failure
- **WHEN** HTTP 请求连接失败，未收到远端结果。
- **THEN** 标记 TRANSPORT_FAILURE 与执行结果未知，不制造 CLI exitCode 1 的已观察证据。

### Requirement: OC-ERR-006 Decoding does not retry writes
The decoder SHALL be side-effect free and SHALL not retry commands based solely on code, help text or trace metadata.

#### Scenario: Retry suggestion
- **WHEN** help 提示登录后重试，或 code 为 SESSION_BUSY/SECURITY_BLOCK。
- **THEN** 只暴露信息，不自动登录、重复提交或执行 help 中命令。

#### Scenario: Readable JSON alternative
- **WHEN** 测试输入为与 YAML 等价的 JSON failure envelope。
- **THEN** 产生相同业务字段和来源信息；其格式来源单独记录，不能把 fixture 当实测上游 JSON 错误输出。
