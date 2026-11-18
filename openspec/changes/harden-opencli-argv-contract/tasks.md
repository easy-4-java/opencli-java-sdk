# C01 Tasks · 参数保真与统一调用契约

> 已勾选项对应真实子进程契约及历史失败证据。保留 3.4 的评审和归档门禁。

关联规范：[spec](specs/opencli-argv-contract/spec.md)。

## 1. 失败测试

- [x] 1.1 在现有 core/support 测试区新增 Java argv echo fixture，覆盖空值、空白、Unicode、换行、-- 和重复 flag。
- [x] 1.2 先在实现前运行新增用例，保存真实失败输出；禁止只断言命令前缀。

## 2. 最小实现

- [x] 2.1 统一 Executor/AdapterChannel/参数 helpers 的值保真路径，并对输入列表做快照。
- [x] 2.2 为 null/标识符错误建立不包含业务值的校验；增量支持有序 option occurrences 和 false 语义。

## 3. 迁移与验证

- [x] 3.1 补 List/varargs/typed/request 等价与用户列表不被修改的测试。
- [x] 3.2 检查 Windows launcher 与引号边界，未测试的平台明确保留未验证状态。
- [x] 3.3 分别在三条 feature 线运行共享 vectors 和现有测试，记录允许差异。
- [ ] 3.4 评审迁移说明与完整 diff；需求通过后再申请 archive，不在文档阶段勾选。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## C01 实现与参数保真证据（2026-11-18）

- OC-ARGV-001、002、004、005。提交 `cdc8b66e74730ab1ed99a9f24a077f9077d2dbf5` 只引入契约夹具。在独立源码目录用 JDK 17 运行 `mvn -B -ntp -o -Dtest=OpenCliArgvContractTest test`，33 项中 21 项失败，0 错误、0 跳过，退出 1。真实子进程收到的完整向量显示空值被丢弃、两端空白被裁剪，typed browser fill 也丢失用于清空字段的空字符串。
- OC-ARGV-003。提交 `d02b16ddc5e0fe7c1d98c6847f4b2999cf2f2566` 用同一环境运行 `OpenCliStructuredArgvContractTest`，10 项中 10 项失败，0 错误、0 跳过，退出 1。可变 Map 与 StringBuilder 改写了实际 argv。当前实现支持显式 schema、有序重复选项、值型 false、否定 flag，拒绝歧义，旧 Map 继续使用历史语义。[历史 RED/GREEN 执行记录](../../../docs/implementation/c10-c01-execution.md)
- 当前本地回归覆盖完整向量、argv 第三项为 null、空白标识符、等号、引号、反斜杠、`--`、Unicode 和换行。新增排队变更回归。一个真实 JVM 占住共享许可，下一次调用完成列表快照后，调用方修改原列表并释放许可。回显子进程收到提交时的完整向量，调用方原列表只包含它自己所作修改。JDK 17 定向 45 项通过。
- Windows 使用原生 `node.exe` 运行回显夹具，含空白、引号和末尾反斜杠的 token 保留。SDK 会在启动前拒绝 `.cmd`、`.bat` 批处理入口。JDK 17 曾运行真实安装包的启动器测试，仅执行只读元命令。对应 [JDK 8 平台 CI](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44339054705)、[JDK 17 Windows](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44339055208/job/134923652889)、[JDK 21 Windows](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44339054801) 与 [已安装启动器证据](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44313779366/job/134834399117)。
- 三条维护线使用同一套已校验 SHA-256 的 `argv.tsv`、`ContractProbe` 和 `OpenCliStructuredArgvContractTest`。JDK 17 使用系统 Maven 执行 `-B -ntp -o clean verify`，2,121 项通过，0 失败、0 错误、0 跳过，退出 0。本地报告为 `target/surefire-reports`。三个版本的 JDK/Jackson/Maven 允许差异见 fixture 的 `sources.lock.json`。远端分支最新 CI 单独记录于 PR。
