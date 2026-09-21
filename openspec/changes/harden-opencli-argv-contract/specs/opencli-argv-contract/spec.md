# 参数保真与统一调用契约

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-ARGV-001 Raw values are lossless
The SDK SHALL preserve each non-null raw argv token exactly, including empty strings, whitespace, Unicode and line breaks; command identifiers SHALL be validated separately.

#### Scenario: Empty and padded values
- **WHEN** 分别通过 List 和 varargs 调用传入 ["demo", "echo", "", "  x  ", "
中文
"]。
- **THEN** 真实子进程收到相同的 5 个 token，顺序与内容完全一致，不丢弃空值。

#### Scenario: Null is rejected
- **WHEN** argv 的第 3 项为 null，或必填 adapter ID 为空白。
- **THEN** 在启动进程之前抛出参数校验失败并指出索引或字段；错误文本不带其它业务 token。

### Requirement: OC-ARGV-002 Overloads share one token contract
The SDK SHALL apply the same value-preservation rules to AdapterChannel, Executor, typed-client and structured-request paths and SHALL not mutate caller-owned lists.

#### Scenario: Caller mutates after submission
- **WHEN** 提交后调用者修改原始 List，任务仍在等待许可。
- **THEN** 本次执行使用提交时的不可变快照，不受到后续修改影响。

#### Scenario: Equivalent overloads
- **WHEN** 相同 token 分别经 List、varargs 与可无损表达的结构化请求提交。
- **THEN** 子进程收到等价序列，参数数目不随入口改变。

### Requirement: OC-ARGV-003 Option occurrences preserve intent
A schema-aware builder SHALL preserve repeated option occurrences and SHALL distinguish absent values, explicit false, flag presence and negated flags.

#### Scenario: Repeated and false-valued options
- **WHEN** 给支持 repeat 的命令传入两次 --tag，并为带值布尔选项设置 false。
- **THEN** 两次 --tag 按顺序保留，false 以该 schema 定义的值形式输出，而不是被当作未提供。

#### Scenario: Unknown option semantics
- **WHEN** 结构化 builder 不知道某字段应为 flag 还是 optional value。
- **THEN** 返回明确的 schema 不足/参数错误；不得猜测或丢弃值。调用者仍可显式使用本地 raw 入口。

### Requirement: OC-ARGV-004 Literal arguments are not shell source
The SDK SHALL execute a literal argv vector without implicit shell expansion and SHALL preserve the argument terminator and negative-looking values.

#### Scenario: Shell-looking value
- **WHEN** raw value 为 "$(printf SHOULD_NOT_RUN); *"，且出现 -- 后的 -literal。
- **THEN** 这些内容只作为字面参数到达 echo 子进程，不执行 substitution，也不自行展开通配符。

#### Scenario: Embedded equals and quotes
- **WHEN** raw token 包含 --key=a=b、引号、反斜杠或中文路径。
- **THEN** 执行层不自行按等号/引号拆分 token；平台特定 launcher 限制在启动前明确报告。

### Requirement: OC-ARGV-005 Evidence reaches the process boundary
Regression tests SHALL assert complete argv at a real local child process boundary on each supported branch; RecordingOpenCliExecutor-only assertions SHALL not close this requirement.

#### Scenario: Prefix-only test passes
- **WHEN** Recording fixture 只检查 adapter/command 前缀，而真实 echo 发现空字符串丢失。
- **THEN** 本要求仍失败，不得以 manifest 用例数将其标记完成。

#### Scenario: Three branch verification
- **WHEN** 1.x、2.x、3.x 分别运行相同无网络 echo vectors。
- **THEN** 报告每条线的 HEAD、JDK、退出码和完整断言结果；未运行的线保持 NOT_RUN。
