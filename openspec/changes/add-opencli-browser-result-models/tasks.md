# C07 Tasks · Browser 领域结果与严格解析

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-browser-result-models/spec.md)。

## 1. 输出证据

- [ ] 1.1 固定第一批leaf的真实输出fixtures、版本/平台/hash；无样本者在支持表标pending。
- [ ] 1.2 先补 malformed/truncated/empty/unknownfields/largeID/AX 的失败测试。

## 2. 模型与decoder

- [ ] 2.1 实现Java8公共模型与各Jackson分支兼容decoder，避免以double中转ID。
- [ ] 2.2 保留raw/JsonNode与旧close返回签名，新增strict访问而非破坏旧API。
- [ ] 2.3 建立每leaf字段约束和parse-state，未验证shape不自动转成空成功。

## 3. 集成验收

- [ ] 3.1 与C03错误/trace、C02截断、C08上下文保持一致，禁止自动读取sitemap路径。
- [ ] 3.2 在三线共享fixtures上验证字段与失败语义等价。
- [ ] 3.3 生成实际typed支持表，区分已验证和仅raw支持，补迁移示例。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## OC-BROWSER-001/002/003/005/006 首批叶子

`OpenCliBrowserResultTest` 首先因缺少结果类型无法编译。加入 tab list 和 open 的叶子模型后，验收真实 CLI action 的归档输出、合法空数组、畸形输出、截断、命令失败、精确 ID 和扩展元数据。两种命令通过现有会话入口运行，原始结果和既有 close 签名保留。见 [样本与复跑方式](evidence/2026-11-19-open-and-tab-list.md)。AX/state、其余叶子与真实浏览器接入继续验收。

### OC-BROWSER-002/005/006 三线失败语义

用已安装的 OpenCLI 1.8.8 和 Node 24.18.0 在隔离页面桩边界重新运行两次 tab list（非空与空）及 open。三份 stdout 与归档样本逐字节一致，Java 8、17、21 分支的样本均通过 `SHA256SUMS`。该验证未连接真实浏览器或外部站点。

`OpenCliErrorEnvelopeTest#typedBrowserListPreservesProcessFailureEvenWithAnEmptyArray` 启动独立 JVM，输出合法空数组和 YAML 错误信封，然后以 1 退出。类型化 tab list 保留原非零退出异常、报告退出码 77 和 trace 元数据。显式对 partial result 解码得到 `EXECUTION_FAILED` 并保留同一原始对象。参数记录只出现一次 tab list，没有触发额外的命令或关闭操作。

在 `3692329`、`f2feb186`、`432dac2` 上加入回归后，用对应 JDK 执行 `./mvnw -B -ntp clean verify`，三线均退出 0。全量分别通过 2215、2216、2215 项。每条线的 Browser 结果 4 项和错误信封 7 项完全一致，零失败、零错误、零跳过。报告在 `target/surefire-reports`。

## OC-BROWSER-001/002/004 state 与 AX

`OpenCliBrowserResultTest` 在缺少 state 类型模型和门面入口时无法编译。实现后验收 DOM 与 AX 文本、对比 JSON、单侧失败、无法识别的输出及 typed null source 拒绝。原有 String source 和 compareSources 继续由既有 Options 传递。见 [四份上游入口样本](evidence/2026-11-19-state-dom-and-ax.md)。

### OC-BROWSER-002 state 文本边界回归

Java 17 基线 `b1d83867` 的定向测试把含重复回车的分隔行误判为 `PARSED`，构建退出 1。修复后，空行只接受 LF 或 CRLF。URL 头含控制字符时按 `INVALID_SHAPE` 返回，保留原始执行结果。正常 CRLF 快照的正文逐字符保留。

以 `4d3d57e`、`b1d83867`、`5242e11` 为基线加入修复后，Java 8、17、21 的 `./mvnw -B -ntp clean verify` 全量分别通过 2219、2220、2219 项。每线执行 8 项 Browser 结果测试，零失败、零错误、零跳过，退出码均为 0。另用 OpenCLI 1.8.8 的真实入口与原隔离页面桩重采四份 state 样本，与归档逐字节一致。

## OC-BROWSER-001/002/003/005 find、console、network 与 analyze

`OpenCliBrowserEvidenceResultTest` 首先因缺少模型和门面而无法编译。实现后验收有限的 find 条目、整数 ref 保真、采集 count 与数组一致性、分析扩展信息和原有执行失败。流式采集及网络详情在 typed 入口启动前拒绝，保留 raw 方法。见 [输出证据、支持表和迁移方式](evidence/2026-11-19-find-console-network-analyze.md)。

### OC-BROWSER-003 console 级别回归

锁定版 CLI 的 console 过滤同时识别 `type` 和 `level`。Java 17 基线 `115b0955` 在仅含 `level` 的 [CLI 输出](evidence/2026-11-19-find-console-network-analyze.md) 上返回空级别，定向测试退出 1。补齐兼容读取后，调用方通过 `getType()` 取得原级别，元数据仍保留原始键。

以 `a0c66c9`、`115b0955`、`7992972` 为基线，在 Java 8、17、21 执行 `./mvnw -B -ntp clean verify`，分别通过 2224、2225、2224 项，全量零失败、零错误、零跳过。每条线均运行 5 项 `OpenCliBrowserEvidenceResultTest`，报告见 `target/surefire-reports`。

## OC-BROWSER-001/002/004 真实 Browser Bridge

`OpenCliBrowserBridgeIT` 使用 Chrome for Testing 158.0.8127.18、OpenCLI 1.8.8 和发布版 Browser Bridge 1.0.24，在 macOS arm64 的临时 Chrome profile 和本地测试页面上验证 open、tab list、DOM、AX、双源对比、find、console、network 和 analyze。Java 8、17、21 各执行 2 项，零失败、零错误、零跳过。详见 [真实 Browser Bridge 验收及复跑条件](evidence/2026-11-19-real-browser-bridge.md)。
