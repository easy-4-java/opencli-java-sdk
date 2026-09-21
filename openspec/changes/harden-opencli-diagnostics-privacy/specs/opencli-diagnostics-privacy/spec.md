# 默认日志与异常的隐私边界

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-PRIV-001 Default logs contain only approved metadata
SDK-owned default logs SHALL use an allowlisted metadata projection and SHALL exclude argv values, prompts, code, stdout/stderr, HTTP bodies and free-form remote error text.

#### Scenario: Failure contains prompt
- **WHEN** 调用参数与子进程stderr包含随机canary secret，进程失败。
- **THEN** 捕获全部SDK日志后搜索不到canary；仍可用correlationId/状态码/字节数定位事件。

#### Scenario: Debug level enabled
- **WHEN** 应用将SDK logger调为DEBUG，AdapterChannel生成调用摘要。
- **THEN** 摘要不打印前几个业务参数，DEBUG本身不等于允许记录业务载荷。

### Requirement: OC-PRIV-002 Exception diagnostics do not reintroduce payloads
Default exception messages and SDK-rendered cause diagnostics SHALL avoid raw command lines, provider body previews, credentials and untrusted parser snippets.

#### Scenario: HTTP503 body secret
- **WHEN** 远端503 body含access token，provider异常也包含该body。
- **THEN** 顶层message与SDK渲染的cause均不出现token；状态503和安全类别保留。

#### Scenario: Parser error location
- **WHEN** YAML/JSON解析器异常嵌入敏感原文片段。
- **THEN** SDK不把未经处理的exception.toString或stack作为默认日志输出。

### Requirement: OC-PRIV-003 Raw results and diagnostic projections are separate
The SDK SHALL retain authorized raw business output without silently redacting or rewriting it to satisfy diagnostic rules, while keeping raw HTTP capture opt-in and disabled by default.

#### Scenario: Business value resembles token
- **WHEN** 正常业务输出中的字段包含token形态字符串。
- **THEN** 已授权raw结果保留原值；默认日志仍不包含它，两条边界分别测试。

#### Scenario: Default HTTP capture
- **WHEN** 调用者没有开启remoteCaptureRawHttpResponse。
- **THEN** rawHTTP调试字段不采集/暴露；常规业务items结果仍按协议可用。

### Requirement: OC-PRIV-004 Sensitive locations are not default identifiers
Credentials, URL queries/user-info and untrusted filesystem paths SHALL not be used as default diagnostic identifiers; safe correlation identifiers SHALL be generated independently.

#### Scenario: Credential-shaped URL
- **WHEN** 配置或服务端错误带含query token/user-info的URL。
- **THEN** 日志/异常只使用安全的可定位部分或摘要，不输出凭据。

#### Scenario: Trace directory contains user data
- **WHEN** trace/sitemap路径含用户名或敏感目录名。
- **THEN** 默认诊断不打印/访问该完整路径；显式受控结果仍可保留metadata。

### Requirement: OC-PRIV-005 Diagnostic export is explicit and finite
Any payload-bearing diagnostic export SHALL require explicit opt-in, enforce finite size and retention policy, and describe redaction limitations without changing command execution.

#### Scenario: No export consent
- **WHEN** 命令失败但调用者未启用导出。
- **THEN** 不自动写raw到磁盘或上传外部系统。

#### Scenario: Enabled bounded export
- **WHEN** 调用者显式选择有限导出策略。
- **THEN** 只产生受控大小的安全投影，标明截断与脱敏策略；执行结果不因导出失败被改写。

### Requirement: OC-PRIV-006 Privacy regressions are tested end to end
The contract suite SHALL test canary secrets across local, HTTP, WS, timeout and parser failure paths, and SHALL treat any SDK-default diagnostic leak as a failing gate.

#### Scenario: Nested cause leak
- **WHEN** 顶层message安全但cause中仍有canary。
- **THEN** 隐私测试失败，不能只检查顶层字符串后通过。

#### Scenario: No diagnostic leak but caller prints raw
- **WHEN** SDK日志无canary，应用自行打印已授权raw。
- **THEN** 不把应用打印作为SDK默认日志通过的反证；报告保证边界而不宣称系统全链路无泄露。
