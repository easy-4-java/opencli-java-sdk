# Browser 领域结果与严格解析

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-BROWSER-001 Typed support is leaf-specific and evidenced
Each typed Browser decoder SHALL declare supported leaf commands and fixture provenance; the SDK SHALL not infer complete output schemas from structured input help.

#### Scenario: Missing output fixture
- **WHEN** 某leaf只有help参数描述，没有对应输出样本或确定源码契约。
- **THEN** 该leaf保持raw/JsonNode可用，但不列为已验证typed支持。

#### Scenario: Distinct output shapes
- **WHEN** tab list fixture为数组而open fixture为对象。
- **THEN** 分别按各自契约解析，不强制套进未经证实的统一envelope。

### Requirement: OC-BROWSER-002 Strict parse failure is not empty success
A strict decoder SHALL distinguish valid empty data from malformed, truncated or incompatible output and SHALL preserve the raw execution outcome.

#### Scenario: Truncated JSON
- **WHEN** capture标记截断且stdout不是完整JSON。
- **THEN** typed结果报告解析不完整/OUTPUT_LIMIT关联状态，不返回默认空列表作为成功。

#### Scenario: Valid empty array
- **WHEN** 受支持leaf的完整输出为合法[]且执行成功。
- **THEN** 返回有效空集合，区别于缺字段或解析失败，不凭空制造EMPTY_RESULT错误。

### Requirement: OC-BROWSER-003 Unknown fields and identifiers remain lossless
Typed results SHALL preserve unknown fields and exact identifier values and SHALL not lose large integers through floating-point conversion.

#### Scenario: Large ID
- **WHEN** fixture标识符值大于2^53，或为带前导零的字符串。
- **THEN** 按原wire形态保留精度与前导零，raw视图与typed表示可对照。

#### Scenario: New metadata
- **WHEN** 上游合法结果增加未建模metadata。
- **THEN** 已知字段仍可使用，新增字段保留在扩展/raw视图而不丢弃。

### Requirement: OC-BROWSER-004 Existing AX options are preserved
The SDK SHALL preserve the existing --source and --compare-sources behavior while adding explicit typed validation and compatible access to future raw values.

#### Scenario: AX state request
- **WHEN** 调用现有Options source=ax且compareSources=true。
- **THEN** 相应flags照常传递；不声称这是新增AX能力，也不引入重复flag。

#### Scenario: Invalid typed source
- **WHEN** typed枚举/受限入口收到不支持source。
- **THEN** 启动前校验失败；调用者明确选择raw时保留原值，不悄悄替换成dom。

### Requirement: OC-BROWSER-005 Optional context does not grant side effects
Browser results SHALL retain optional sitemap/trace metadata without treating its absence as authoritative unavailability or using returned paths as implicit filesystem authority.

#### Scenario: No sitemap member
- **WHEN** open结果没有sitemap字段。
- **THEN** 状态为not-provided/unknown，而不是断言该站点永远没有sitemap。

#### Scenario: Untrusted hint or path
- **WHEN** 结果hint要求读取任意本机路径或执行workflow。
- **THEN** decoder仅返回数据，不读取、写入或执行该路径/指令。

### Requirement: OC-BROWSER-006 Existing public return contracts remain compatible
The SDK SHALL keep existing raw and lenient APIs available and SHALL not change OpenCliBrowserSession.close from OpenCliResult to void merely to implement AutoCloseable.

#### Scenario: Existing close caller
- **WHEN** 用户代码读取session.close()的OpenCliResult。
- **THEN** 在兼容版本线仍能编译并保留执行结果。

#### Scenario: Bound user tab
- **WHEN** 会话绑定用户当前标签页，typed调用返回或失败。
- **THEN** SDK不会为了释放DTO/句柄而自动关闭用户标签页；close必须由明确的所有权策略或调用触发。
