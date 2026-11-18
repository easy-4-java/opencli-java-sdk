# C08 Tasks · 隔离执行上下文与只读诊断

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-context-diagnostics/spec.md)。

## 1. 上下文契约

- [x] 1.1 核实锁定版本profile与Browser公共options的scope和真实输出fixture。见调用上下文证据。
- [ ] 1.2 编写并发context隔离、false覆盖、scope冲突与诊断UNKNOWN的失败测试。

## 2. 增量API

- [ ] 2.1 实现不可变context与优先级合并，复用原meta/auth/skills入口。
- [x] 2.2 实现per-check只读诊断，审计每个probe副作用；不能确认时保留unknown。
- [x] 2.3 保留sitemap raw/optional metadata，不自动访问overlay或trace文件。

## 3. 边界评审

- [x] 3.1 证明诊断不会执行login/refresh/restart/install/profile use等变更操作。
- [x] 3.2 更新DEFERRED清单，移除未经证实的pipelineCLI/单进程承诺。
- [x] 3.3 三线验证上下文与诊断状态一致，并在指南中给出明确的只读使用示例。见三线只读验证。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## OC-CTX-001/002 不可变上下文与作用域

提供不可变 `OpenCliInvocationContext` 与 `OpenCliProperties.invocationDefaults`。调用的显式值覆盖默认值，false 会生成有值的 `--keep-tab false`。冲突的显式选项或当前范围不支持的参数在业务进程启动前拒绝。Browser 全局、命名空间和叶子选项分别绑定。见 [锁定版证据与使用示例](evidence/2026-11-19-invocation-context.md)。

`OpenCliInvocationContextTest` 首先因缺少 API 无法编译。实现后用 JVM 子进程验证并发 profile/window、false 与默认继承、非 Browser scope 拒绝及显式冲突。仍需验收诊断与其只读边界。

### OC-CTX-001 调用开始时固定位置参数

在独立用户目录中以 OpenCLI 1.8.8 复采 `profile`、`zhihu hot` 和 `npm search` 的帮助，与归档逐字节一致。profile 帮助声明 global `--profile`，浏览器适配器帮助声明 `browser_common_options`，无浏览器适配器 npm 不声明同组参数。

Java 17 基线 `2eb97e49` 的独立 JVM 定向测试在帮助探测暂停期间替换调用者列表的元素，原实现随后执行了新参数，测试退出 1。修复后，在启动探测前固定位置参数，继续由绑定器根据观测到的 schema 校验。本次执行使用初始值。

以 `68c5b19`、`2eb97e49`、`5a464a3` 为基线加入修复，在 Java 8、17、21 执行 `./mvnw -B -ntp clean verify`，全量分别通过 2228、2229、2228 项。每条线均执行 15 项发现测试和 3 项上下文测试，零失败、零错误、零跳过。报告见 `target/surefire-reports`。


## OC-CTX-003/004/005/006 只读诊断

`diagnose()` 独立报告可执行文件、版本、历史帮助、Browser Bridge、认证、daemon 和远端传输。历史帮助有来源和观察时刻，配置或版本不匹配时保持 UNKNOWN。远端只读诊断跳过本机命令并保持传输未验证。三个本地运行时状态无法确认只读探测，保持 UNKNOWN。见 [只读边界与集成验证](evidence/2026-11-19-read-only-diagnostics.md)。

`OpenCliDiagnosticsTest` 初始缺少公开 API，编译失败。实现后回归记录只运行 `--version`，并验证显式发现的历史证据不提升为当前健康。已发布 CLI 的 opt-in `OpenCliReadOnlyDiagnosticsIT` 区分版本探测与执行启动钩子的显式帮助。报告位于 `target/surefire-reports`。

### 三线只读验证

以 `afeb522`、`3c9979ec`、`5c35d33` 为基线，使用 Java 8、17、21 执行 `./mvnw -B -ntp clean verify`，分别通过 2231、2232、2231 项，零失败、零错误、零跳过。每条线均执行 3 项 `OpenCliDiagnosticsTest`。配置 Node 24.18.0 与实际 OpenCLI 1.8.8 后，分别执行 `./mvnw -B -ntp -Dtest=OpenCliReadOnlyDiagnosticsIT test`，每条线均执行并通过 1 项。报告位于 `target/surefire-reports`。调用方式与各检查的证据类型见[只读诊断示例](evidence/2026-11-19-read-only-diagnostics.md)。
