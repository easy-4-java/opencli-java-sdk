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
