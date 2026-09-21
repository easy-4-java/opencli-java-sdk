# 共享契约测试、分支差异与上游锁定

> Proposal specification. Status: DRAFT / NOT_IMPLEMENTED.

## ADDED Requirements

### Requirement: OC-PARITY-001 Branch differences are explicit
The project SHALL maintain an allowed-differences matrix for Java, Jackson, Maven and platform adapters while enforcing identical externally observable command semantics for shared fixtures.

#### Scenario: Jackson major divergence
- **WHEN** 1.x/2.x使用Jackson2，3.x使用tools.jackson3。
- **THEN** 允许imports/decoder实现差异，但同一fixture的字段、错误原因和unknown处理一致。

#### Scenario: Behavior drift
- **WHEN** 某分支对空字符串/false/timeout有不同结果。
- **THEN** 契约门禁失败，即使该分支单独mvn test或覆盖率报告通过。

### Requirement: OC-PARITY-002 Upstream baselines are reproducible
Every promoted upstream contract snapshot SHALL include version/ref when available, content hashes, acquisition command, observation time and scope, without depending on an author's absolute local path.

#### Scenario: Historical fixture
- **WHEN** 读取仅含1275/173和作者本机manifestPath的旧metadata。
- **THEN** 标为历史来源不足，不能将它用作最新上游已验证证据。

#### Scenario: Updated snapshot
- **WHEN** 引入新的help/manifest/outputfixture。
- **THEN** 内容hash与来源元数据一起变更，并产生逐项diff供评审，不只比较总数。

### Requirement: OC-PARITY-003 Coverage dimensions are not conflated
Coverage reports SHALL independently report enumeration, argv, protocol, typed-result and real-execution evidence with explicit denominators and NOT_RUN states.

#### Scenario: Many mock cases
- **WHEN** 1275个Recording测试通过，但没有真实CLI运行。
- **THEN** 仅相应mock/argv构造层可记通过；live层仍NOT_RUN，不能展示整体100%可用。

#### Scenario: Scope changes
- **WHEN** 用户安装插件或本地override，命令集合超出bundled快照。
- **THEN** 报告观察范围与未知扩展，不把缺少枚举记录当作这些命令不存在。

### Requirement: OC-PARITY-004 CI success requires execution evidence
A branch verification SHALL record exact HEAD, actual tool versions, test and skip counts, exit code and artifacts; untriggered jobs, missing reports or zero executed tests SHALL not count as successful SDK verification.

#### Scenario: Docs-only branch
- **WHEN** 提交规范但workflow没有因该分支触发。
- **THEN** 状态为NOT_RUN/NOT_TRIGGERED，而不是CI绿色通过。

#### Scenario: Wrong Maven for 3.x
- **WHEN** 使用不能解析POM4.1.0的系统Maven运行3.x。
- **THEN** 报告工具链失败并使用该线wrapper重新验证，不能跳过构建称兼容。

### Requirement: OC-PARITY-005 Documentation follows observed branch facts
README, examples and POM descriptions SHALL agree with the declared and tested branch baseline and SHALL not advertise pending changes as shipped capabilities.

#### Scenario: Conflicting Java versions
- **WHEN** 3.x文档标题写Java21，正文写JDK8可编译。
- **THEN** 文档检查失败，按3.x实际声明和验证记录修正。

#### Scenario: Draft capability
- **WHEN** 本提案定义strictBrowser模型但尚未实现。
- **THEN** 功能表列为PLANNED/DRAFT，不修改发布说明声称available。

### Requirement: OC-PARITY-006 Proposal validation and implementation closure are separate
A change SHALL remain unimplemented until reviewed requirements are backed by branch-specific test evidence; OpenSpec syntax validation SHALL not by itself close implementation tasks or promote authoritative specs.

#### Scenario: Format check passes
- **WHEN** proposal/design/spec/tasks结构检查通过。
- **THEN** 只记录文档格式结果，所有未实施任务继续未勾选。

#### Scenario: Final integration closure
- **WHEN** C01–C09选定范围完成，三线报告齐备且官方strict校验通过。
- **THEN** 记录每项Requirement到测试/commit的链接，经评审后再archive，不能提前移动为已生效规格。
