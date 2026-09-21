# 能力现状、优化范围与变更映射

> 提交前状态更新见 [提交前复核补记](opencli-java-sdk-publish-check.md)。历史基线的 coverage 缺失与 1275 计数不再代表当前分支；拟实施契约不因目录补回而自动完成。

> 状态：DRAFT。事实基线见 [复核报告](opencli-java-sdk-reverification.md)，符号来源见 [证据索引](opencli-java-sdk-evidence.md)。本表不使用未经测量的覆盖率。

## 1. 能力矩阵

| 能力 | 已核对的现状 | 实际需要补充 | 优先级 / Change |
|---|---|---|---|
| 任意 Adapter 调用 | 已有 `adapter(id)` / AdapterChannel / 结构化请求入口（E02） | 保留参数值、空字符串、重复选项与显式 false；不要新增同义门面 | P0 C01 |
| 本地执行 | 已有 Commons Exec、watchdog、统一异常（E01/E04） | 稳定许可、排队 deadline、取消清理、有界 capture | P0 C02 |
| 错误模型 | SDK 有进程异常与 JSON shape hint；上游 Adapter stderr 为 YAML envelope（E08/U04/U05） | YAML/JSON/纯文本解析，help/trace、实际 exit 与 payload exit 分离 | P0 C03 |
| HTTP 远程执行 | 对接 opencli-admin legacy `/collect`（E05/E06） | 显式可表达子集、拒绝有损转换、传输状态和真实 exit 来源 | P0 C04 |
| 反向 WS | 已有注册、重连、接单和结果回复（E07） | READY gate、有界队列、request_id 去重、代际/关闭治理 | P0 C05 |
| Structured Help | 上游已有命名空间和命令参数 schema（U02/U03） | 来源可追溯的快照、保守校验、UNKNOWN/STALE 状态 | P1 C06 |
| Browser AX | SDK 已有 `source`、`compareSources`（E03） | 参数约束、按 leaf 的结果 fixture/decoder；不是再增加 AX flags | P1 C07 |
| JSON / typed result | 已有 `OpenCliTypedResult<JsonNode>`（E09） | 强类型领域结果与严格解析，不把 text fallback 当成功模型 | P1 C07 |
| 执行上下文 | 上游可传 profile/window/trace/site-session/keep-tab（U02/U05） | 单次不可变上下文与无全局副作用的合并 | P1 C08 |
| Auth / Skills / Meta / External | 已有对应门面（E12）；未重跑每个方法 | 复用入口，核对 schema/只读诊断；不默认安装、刷新或重命名 API | P1 C06/C08 |
| Sitemap | 上轮发现上游提示，本轮未取得完整输出 fixture | 先保留可选 metadata；正式 DTO 要先取 fixture；文件读写管理暂缓 | P1 C07/C08 |
| Pipeline | 已证实 JS package export（U01/U06） | 独立 ADR 证明接入路径后再决定；不虚构公共 CLI 命令 | DEFERRED |
| 日志与诊断输出 | 当前若干日志/异常会输出 argv、HTTP preview、error（E01/E02/E06/E07） | 默认元数据白名单，raw 业务结果与诊断投影分离 | P0 C09 |
| coverage | 1.x 五个 coverage 类；1275/173 历史 fixture；示例测试检查 argv 前缀（E10/E11） | 完整 argv、失败用例、三分支共享契约，独立报告各层覆盖 | P0/P1 C10 |
| 分支兼容 | 8/17/21，Jackson2/3，Maven3/4 有真实差异（E13–E15） | 允许差异表和相同业务 fixture；修正文档与版本声明 | P0/P1 C10 |

## 2. 覆盖统计的定义

以后产生覆盖报告时必须分别列出以下分母、分子和未验证项，禁止将它们合并成一个营销数字。

- **目录枚举覆盖**：锁定快照中的命令是否有记录；分母由 snapshot hash 确定，不等于本机自定义扩展全集。
- **argv 构造覆盖**：每个命令/选项的正例与负例是否通过真实参数回显或完整 token 断言。
- **协议兼容覆盖**：Help、错误、HTTP、WS fixture 是否保持信息和失败语义；不同 transport 单列。
- **typed 结果覆盖**：哪些 leaf 有经过 fixture 验证的 decoder，哪些仅提供 JsonNode/raw。
- **真实运行覆盖**：在明确版本、平台和账户条件下实际跑过的命令；未运行项保持 NOT_RUN，不能借用前四层结果。

历史 `1275` 与 `173` 只能标注为 SDK fixture 记录，不替代本次未完成的上游最新 manifest 逐条采集。

## 3. 上轮十项建议如何落地

| 上轮建议 | 本轮归宿与调整 |
|---|---|
| capability-discovery | C06；降到正确性修复之后，保留 UNKNOWN/STALE，避免“全自动 100% 对齐”的承诺 |
| command-coverage-guard | C10；复用现有测试而非盲目复制，提升断言质量 |
| structured-error-envelope | C03；以 stderr YAML / wire help / trace 为实际契约 |
| browser-typed-results | C07；承认已有 JsonNode 与 AX，采用增量 decoder |
| sitemap-support | C07/C08 的可选 metadata；完整知识库管理暂缓 |
| dynamic-adapter-runtime | 通用调用已存在；C01/C06 修复与增强，不重复造入口 |
| pipeline-support | 从强制实现清单移除；未来另立 ADR，不能从 JS export 推导 CLI |
| transport-abstraction | C04 小范围分离 Local 与 legacy collect；C05 独立 Worker，不假称第三个应用端 transport |
| runtime-compatibility | C06/C08/C10，区分可执行文件可见、schema 兼容、真实运行成功 |
| upstream-contract-ci | C10，共同 fixture、上游锁定、分支允许差异、真实 CI 证据 |

新增的主要修复是 C01 参数保真、C02 资源生命周期、C05 反向 Worker 治理、C09 默认诊断隐私。它们来自执行链具体源码，不是为了扩充产品范围。

## 4. 关闭条件

每个 Change 必须在 `tasks.md` 和验收矩阵中记录测试证据。源码问题的跨分支范围以实际检查为准：F01 本轮三个分支均核对，部分其它风险主要依据 3.x。实施时必须对另外两条线逐项确认，不使用“代码看着相似”代替测试。
