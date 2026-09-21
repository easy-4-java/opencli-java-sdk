# OpenCLI Java SDK 三分支复核报告

> 审计日期：2026-09-21（按用户任务日期，UTC+08:00）。状态：DRAFT / REVIEW_REQUIRED。
> 本报告替代上一轮分析中证据不足的判断；不是发布认证，也不是代码实现完成报告。

> 提交版补记：下文冻结的是原始审计快照，并非当前分支 HEAD。提交前再次核对发现 coverage 已补回、计数与来源已更新；当前状态以 [提交前复核补记](opencli-java-sdk-publish-check.md) 为准，历史证据和锁文件保留不改。

## 1. 范围、方法和交付边界

核对对象是 `easy-4-java/opencli-java-sdk` 的三个 feature 分支；能力对齐对象固定为 `jackwener/OpenCLI`。本轮重新读取 GitHub 分支、POM、关键执行链、参数转换、HTTP、反向 WS、JSON 解析、AX Options、coverage 测试和上游 Help/error/pipeline 源码。每项结论引用同目录 [证据索引](opencli-java-sdk-evidence.md)，具体 commit/blob 保存在 [sources.lock.json](opencli-java-sdk-sources.lock.json)。

上一轮已通过本机工具 fetch 和比较分支；**本轮不能延用那次本机连接成功作为本轮执行证据**。本轮 WebCodex 工具发现未返回可用工具；容器 git clone 发生 `Could not resolve host: github.com`；npm 读取发生 `EAI_AGAIN registry.npmjs.org`。因此本轮是 GitHub 固定快照的针对性源码复核，未完成本地 CodeGraph 索引和完整三分支重新构建。

本包交付架构、事实复核、能力矩阵与 OpenSpec 提案。原始生成阶段没有改 Java、测试、POM、现有 CI，没有提交或推送，也没有把文件写到用户 Mac 的项目目录；后续授权提交的范围与基线见上述补记。所有验收测试在规范中均为待执行任务。

## 2. 冻结的事实基线

| 版本线 | 精确 HEAD | POM 声明 JDK | POM 声明 Maven | Jackson / 模型 |
|---|---|---:|---|---|
| feature/1.0.x | `2095f617d56789569d32cffe2cdedffe595270ba` | 8 | 3.9.16 | Jackson 2.18.9 / POM 4.0.0 |
| feature/2.0.x | `26d703c72389361e1b755733ac61145f783334b5` | 17 | 3.9.16 | Jackson 2.22.1 / POM 4.0.0 |
| feature/3.0.x | `a1278008c17fd94523ad47307183485fefdf67e0` | 21 | 4.0.0-rc-5 | Jackson 3.2.1 / POM 4.1.0 |

来源：E13–E15。表中是源码声明，不是本机实际执行版本。3.x 的 Jackson databind 命名空间为 `tools.jackson`；注解中仍存在 `com.fasterxml.jackson.annotation`，不能进行不加区分的全文替换。3.x CI 明确使用 checked-in wrapper，不能套用系统 Maven 3（E06、E15、E16）。

上游固定为 `8271afc67e8504bda94c147f446ee29775d08274`，`package.json` 为 **1.8.8**；分支 API 对应 release commit 时间为 2026-08-30T17:35:37Z。本文不将 SDK 的 1.x/2.x/3.x 当成上游 OpenCLI 的版本（U01）。

## 3. 对上一轮判断的正式纠正

| 上轮表述或隐含结论 | 本轮核实结果 | 本规范处理 |
|---|---|---|
| Dynamic Adapter Client 缺失，需要从零新增 | `OpenCliAdapterChannel.invoke(...)` 与 `OpenCliAdapterCommandRequest` 入口已存在 | 保留已有入口；补参数保真、schema、范围校验，不另造同义门面（E02） |
| AX State 尚未支持 | `source` 和 `compareSources` 已组装相应 flags | 改为 Options 约束、领域结果与真实契约测试，不重复实现（E03） |
| Structured Error 可按 JSON + hint 处理 | wire 字段是 `error.help`；Adapter 实际输出为 **stderr YAML**，可带 AutoFix 注释 | YAML 为当前重点；兼容 JSON 与纯文本，保留 trace（U04、U05） |
| 1,275 条命令的覆盖意味着所有命令可用 | 这是 SDK 保存的快照计数；coverage 测试使用 Recording executor，仅验证 argv 前两项 | 不声称真实网站执行通过，不声称 100% 语义覆盖（E10、E11） |
| 2.x/3.x 删除 coverage，因而整个测试体系不足 | 本轮确认指定 coverage 目录在两线返回 404；不能据此断言没有其它测试或断言删除动机 | 对齐同一 contract suite，保留 existing tests，不以行数评价整体安全性 |
| Pipeline 导出意味着可增加 Java pipeline CLI | `./pipeline` 是 JS 模块导出；本轮没有建立公共 CLI 调用契约 | 暂不加入强制对齐范围；独立扩展须另立 ADR（U01、U06） |
| HTTP 与反向 WS 是同一个官方远程调用面 | HTTP `/collect` 及反向 Worker 对接 opencli-admin；后者是接单执行本地 CLI 的边缘客户端 | 与上游 OpenCLI 区分；legacy transport 能力显式受限（E05–E07） |
| 可以把所有分支统一用一套依赖源码直接复制 | 三线 Java/Jackson/Maven 均有差异 | 行为契约统一、兼容实现隔离；不覆盖整份 POM（E13–E15） |
| 已做等价 CodeGraph 审计 | 上轮没有 CodeGraph 运行证据，本轮也未执行 | 取消“等价”表述；索引/调用图验证保持未完成 |
| capability schema 可直接生成全部返回 DTO | Help 提供输入参数、命令树等，不保证完整输出 JSON Schema | 输出模型从实际输出 fixture 与源码建立（U02、U03） |

## 4. 确认的缺陷与风险

本节区分源码事实与推导风险。复现输入均是建议的回归向量，**本轮未运行 SDK 的 Java 回归测试**。

### F01 · P0 · 参数值被静默改写（已确认三个分支）

`invoke(String...)` 和 `appendCleanArgs` 使用 `isNotBlank` 与 `trim`；Adapter 通道也重复此处理。`"  保留前后空格  "` 会变化，`""`、只含空格的值会消失。空文本可能是合法的 fill 值；提示词、签名字符串、代码片段也不能由传输层修改。源码的 argv 列表调用并非 shell 字符串执行，不应把此问题误称为已证实的 shell 注入。

回归向量：空字符串、纯空白、前后空格、换行、Unicode、`--` 后以 `-` 开头的字面值、`--key=value` 内含 `=`。断言实际子进程收到的参数数量、顺序和内容一致，而不只看 Recording executor。来源 E01、E02、E17、E18。对应 C01。

### F02 · P0 · 全局并发限制会被新 Client 替换

构造 Executor 会配置静态 `AtomicReference<Semaphore>`；再次配置会替换 Semaphore。旧执行仍持旧许可，新调用拿新许可，应用可能存在两个有效许可池。多个 Client 不能互相覆写并发配置；这是从 E01/E04 控制流推导的风险，尚未实测并发峰值。

另外 `acquire()` 不计入 commandTimeout，排队可无限等待；`maxConcurrent<=0` 实际恢复 CPU 派生默认值，并不是 README 写的 unlimited。对应 C02、C10。

### F03 · P0 · 输出内存无界、中断清理不足

两个 `ByteArrayOutputStream` 没有字节上限。命令有超时不代表输出量有界；`network --follow` 等连续输出尤其需要明确采集模式。`awaitResult` 被中断时没有 SDK 显式销毁进程的 finally，外层只恢复线程中断标记；进程可能继续到 watchdog 超时。不能据此宣称已实测永久孤儿进程。

使用单调时钟、统一 deadline、有限 capture、部分结果以及明确资源所有权；不能为清理一次调用而终止共享 OpenCLI daemon 或无关用户进程。来源 E01/E04。对应 C02。

### F04 · P0 · legacy collect 无法无损表达任意 argv

Parser 固定前两项为 site/command；`[browser,s,state]` 会把会话 `s` 当成 command。Map 会覆盖重复 option；`--limit -1` 的值可能误判为 flag；独立 `--` 会触发 empty option key；trim/lowercase 也会改变语义。Root 单词命令和 External passthrough 不应被默认为 legacy collect 所支持。

优先保留 legacy 协议、建立明确能力白名单，并在不可保真时于发 HTTP 前拒绝。不擅自修改未读取的 opencli-admin server 或虚构 `/execute` endpoint。来源 E05/E06。对应 C04。

### F05 · P0 · 结果与错误信息被压扁

本地 heuristic parser 只识别 JSON 外形。HTTP envelope 仅读取 success/items/error，映射为 exitCode 0/1；这不是远端进程真实退出码。HTTP 非 2xx、传输超时、CLI 超时与业务失败需要不同来源标签。上游 Adapter 错误是 stderr YAML，wire help 与 trace 都不应丢失。来源 E06/E08/U04/U05。对应 C03、C04。

从上游 `resolveExitCode` 的 instanceof 检查和 `toEnvelope` 的 cross-package duck typing 可推导：某些跨包错误的 envelope.exitCode 与进程 exitCode 可能不一致。SDK 应保留两者和来源，而不是篡改任意一方让它们看起来一致。

### F06 · P0 · 反向 Worker 缺少有界任务治理

WS collectPool 是 cachedThreadPool；进程 Semaphore 不能阻止许多线程同时排队。`collect` 分支不验证 register ack 已完成；request_id 可以为空，没有在途/已完成去重；stop/close 没有完整的有界 drain 和重复消息语义。一个已 close 的实例重新 start 也可能面对已 shutdown 的线程池。源码 E07 支持这些控制流观察，不等于已做负载与断线实测。

要求注册状态机、有限队列、过载失败、请求 ID 校验、进程内有界去重、连接代际隔离和终结式 close。不能承诺进程重启后的 exactly-once。对应 C05。

### F07 · P0 · 默认日志和异常可能泄露业务内容

完整 commandLine、前几项参数摘要、HTTP bodyPreview、remote error 都可能带提示词、代码、授权信息或个人数据。上游网络捕获是否脱敏，不能替代 Java 日志治理。按允许的元数据字段记录，而不是只靠少量 key 的正则脱敏。

不要偷偷删改返回给已授权调用方的业务数据。日志安全投影与原始结果访问是两条边界；现有 raw HTTP debug 默认关闭应保留。来源 E01/E02/E06/E07/E09。对应 C09。

### F08 · P1 · 已有通用入口，缺少输入契约与运行时发现

Structured Help 能补 required、takes_value、negate、choices 和命名空间层级。不应从全部任意 argv 反向猜 schema，也不应把快照 173 个站点当成永久全集。缺失或陈旧的 schema 应标记 UNKNOWN/STALE；raw 本地入口作为明确的兼容逃生口保留。来源 E02/E10/U02/U03。对应 C06。

### F09 · P1 · JsonNode 包装不等于 Browser 领域模型

现有 `OpenCliTypedResult<JsonNode>` 有价值；不必删除。新增严格 decoder 与按命令划分的 DTO，保留 raw 与 unknown fields，区分解析失败、空数据和 CLI 失败。AX 参数已经存在；结果模型不能通过再增加一组 flags 来替代。来源 E03/E09。对应 C07。

### F10 · P1 · 全局设置和单次调用上下文需要分离

上游 Help 区分 root、namespace、command options；支持 profile 传递、window、site-session、keep-tab 和 trace。调用 A 的 profile/window 不能修改调用 B；不能为了选 profile 自动执行修改全局默认值的 `profile use`。诊断默认只读，不自动 login、auth refresh、daemon restart、plugin install。来源 U02/U03/U05、E12。对应 C08。

### F11 · P1 · SiteMap 只能先做有证据的 metadata 集成

上轮已发现 open/analyze 的 sitemap 提示；本轮把“Java 站点知识库管理 API”降为可选扩展。本轮未重新获取每个 sitemap 输出的完整 wire fixture，因此仅规划保留可选提示及未知字段，具体 typed 字段须在 C08 获取真实 fixture 后固定。不能因为提示存在就自动读取本机路径、写 overlay 或执行其中的工作流。

### F12 · P0/P1 · coverage、基线和分支治理不闭环

1.x 已有五个 coverage 类值得复用，但现有 adapter coverage 只验证 argv 前缀。2.x/3.x 对应目录不存在不等于其它测试缺失。恢复应改成完整参数断言、负例与锁定上游的 schema drift gate，不能只是复制大量测试获得数量。

Meta fixture 中本机绝对路径无法标识可复现上游版本；新基线应记录 packageVersion、commit、内容 hash、采集命令、采集时刻和来源。当前 1,275/173 仅作为 historical snapshot 记录。来源 E10/E11、coverage 目录观察。对应 C10。

### F13 · P1 · README/POM 的事实漂移

3.x README 同时声称 Java21 与可在 JDK8 编译，列 Jackson2.17.x；Gradle 示例含多余 `.x`；POM description 仍写 2.x/JDK17。新文档必须以各 ref POM 与真实验证报告为依据，不能复制上一轮文案。来源 E12–E15。对应 C10。

## 5. 优先级与关闭标准

P0 不是“最有新意的功能”，而是参数正确性、资源有界、错误保真、远程执行边界与敏感数据保护。先执行 C01/C02/C03/C04/C05/C09 的修复；C10 的契约测试基础可以提前落地，收尾门禁在其它变更完成后关闭。C06/C07/C08 再提升使用体验与可维护性。

问题关闭必须关联 Requirement ID、真实测试、目标分支 commit 和退出结果。代码阅读只是提出问题的证据；文档校验只是规范结构的证据；二者都不能代替实现测试。
