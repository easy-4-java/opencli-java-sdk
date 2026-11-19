# C07 Tasks · Browser 领域结果与严格解析

> 已核对声明支持的叶子及各自的实现和证据。PR 评审与合并仍待完成。

关联规范：[spec](specs/opencli-browser-result-models/spec.md)。

## 1. 输出证据

- [x] 1.1 固定第一批leaf的真实输出fixtures、版本/平台/hash；无样本者在支持表标pending。
- [x] 1.2 先补 malformed/truncated/empty/unknownfields/largeID/AX 的失败测试。

## 2. 模型与decoder

- [x] 2.1 实现Java8公共模型与各Jackson分支兼容decoder，避免以double中转ID。
- [x] 2.2 保留raw/JsonNode与旧close返回签名，新增strict访问而非破坏旧API。
- [x] 2.3 建立每leaf字段约束和parse-state，未验证shape不自动转成空成功。

## 3. 集成验收

- [x] 3.1 与C03错误/trace、C02截断、C08上下文保持一致，禁止自动读取sitemap路径。
- [x] 3.2 在三线共享fixtures上验证字段与失败语义等价。
- [x] 3.3 生成实际typed支持表，区分已验证和仅raw支持，补迁移示例。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## OC-BROWSER-001/002/003/005/006 首批叶子

`OpenCliBrowserResultTest` 首先因缺少结果类型无法编译。加入 tab list 和 open 的叶子模型后，验收真实 CLI action 的归档输出、合法空数组、畸形输出、截断、命令失败、精确 ID 和扩展元数据。两种命令通过现有会话入口运行，原始结果和既有 close 签名保留。见 [样本与复跑方式](evidence/2026-11-19-open-and-tab-list.md)。AX/state、其余叶子与真实浏览器接入继续验收。

## OC-BROWSER-001/002/004 state 与 AX

`OpenCliBrowserResultTest` 在缺少 state 类型模型和门面入口时无法编译。实现后验收 DOM 与 AX 文本、对比 JSON、单侧失败、无法识别的输出及 typed null source 拒绝。原有 String source 和 compareSources 继续由既有 Options 传递。见 [四份上游入口样本](evidence/2026-11-19-state-dom-and-ax.md)。

## OC-BROWSER-001/002/003/005 find、console、network 与 analyze

`OpenCliBrowserEvidenceResultTest` 首先因缺少模型和门面而无法编译。实现后验收有限的 find 条目、整数 ref 保真、采集 count 与数组一致性、分析扩展信息和原有执行失败。流式采集及网络详情在 typed 入口启动前拒绝，保留 raw 方法。见 [输出证据、支持表和迁移方式](evidence/2026-11-19-find-console-network-analyze.md)。

## C07 实施核对与绑定标签页

- OC-BROWSER-001 / OC-BROWSER-002。版本锁定的值、状态与证据表覆盖已声明的 typed 入口。合法空数组、畸形结果、截断和进程失败各有独立状态。
- OC-BROWSER-003。测试保留大于 IEEE 754 安全整数范围的 ID、前导零及未知字段。
- OC-BROWSER-004。真实 Browser Bridge 运行 DOM、AX 和双源对比。参数回归检查原有 source 和 compare-sources，未知 raw source 原样传入。
- OC-BROWSER-005。分析提示、sitemap 和 trace 保留为数据。相关 decoder 不读取文件或执行提示。
- OC-BROWSER-006。已有 close 调用继续返回原始结果。新增真实扩展测试绑定隔离浏览器的活动标签页，验证 typed 成功、请求不存在的目标页失败，以及客户端关闭后，原标签页仍能通过同一 ID 列出。最后显式解绑。

以回环 HTTP 服务的 `/bound-fixture.html` 启动临时 Chrome profile，让它成为活动标签页。该 URL 可以返回与普通 fixture 相同的 HTML。

本分支以 `97aee9a451facfdc998ef039a59f7fa4f4e27476` 为基线，使用 JDK 21 执行 `./mvnw -B -ntp -o -Dtest=OpenCliBrowserBridgeIT,OpenCliBrowserResultTest,OpenCliBrowserEvidenceResultTest,OpenCliErrorEnvelopeTest test`。退出码为 0。运行 24 项，包含 3 项真实 Browser Bridge 用例，零失败、零错误、零跳过。报告位于 `target/surefire-reports`。外部认证网站验收仍保持独立。
