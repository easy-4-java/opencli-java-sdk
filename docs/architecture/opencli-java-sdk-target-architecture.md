# OpenCLI Java SDK 目标架构与演进约束

> 提交前状态更新见 [提交前复核补记](opencli-java-sdk-publish-check.md)。历史基线的 coverage 缺失与 1275 计数不再代表当前分支；拟实施契约不因目录补回而自动完成。

> 提案状态：DRAFT / REVIEW_REQUIRED。本文的模型、默认值和接口草案是设计决定，不是现有 SDK 已实现能力。
> 事实依据见 [复核报告](opencli-java-sdk-reverification.md) 与 [证据索引](opencli-java-sdk-evidence.md)。

## 1. 定位与目标

保留现有 SDK 作为 Java 对 OpenCLI CLI 的可维护调用层；优先修复调用语义，再增加 capability-aware 的校验与结果模型。它不是 OpenCLI browser engine、不是站点 Adapter 的 Java 重写，也不是 Agent 工作流调度平台。

上游 OpenCLI 的 CLI/Help/error/output 是第一边界；opencli-admin 的 collect 与反向 Worker 是第二边界；Java SDK 的资源、日志、配置、异常和类型系统是第三边界。三个边界分别版本化、分别验证，不能混用名称“官方远程协议”。

## 2. 当前实际调用链

```text
Java 应用
  ├─ Typed Adapter / Meta / BrowserSession
  └─ OpenCliAdapterChannel（已有通用入口）
                 │
          OpenCliExecutor
             ┌───┴────────────────────────────────┐
             │ LOCAL_PROCESS                      │ REMOTE_AGENT_HTTP
             ▼                                    ▼
  SubprocessExecutionSupport             ArgvToCollectParser
  Commons Exec / watchdog                site+command+Map+positionals
             │                                    │
             ▼                                    ▼
        OpenCLI CLI                         opencli-admin /collect

中心 opencli-admin ── WebSocket ──▶ ReverseAgentClient
                                     │ 注册/接单/回复
                                     ▼
                                本地 OpenCliExecutor
```

反向 WS 在当前结构中是 Worker：它接收中心的命令并执行本地 CLI，不是供 Java 应用主动发任意命令的第三个 transport。此判断来自 E01/E05/E06/E07。

## 3. 选型：保留 API 的增量改造

| 方案 | 优点 | 代价与判断 |
|---|---|---|
| 继续只增加手写 Client | 改动少，短期能补某些方法 | 无法解决现有 argv/远端/资源问题；不采用作为主路线 |
| 一次性重写为全动态平台 | 概念统一 | API 与依赖破坏面大；schema 也没有完整输出契约；不采用 |
| 保留门面，增加保真执行核、能力描述和有界适配层 | 旧调用可迁移，缺陷可逐个关闭，三线共用行为规范 | 需要严格回归和 compatibility adapter；本提案采用 |

不为“统一命名”额外添加 `tools().tool()` 与 `adapters().site()` 等同义门面。复用 `adapter(id)`、现有 meta/external/browser API；确需新增时采用增量方法而不是删除旧入口。

## 4. 目标分层

```text
                    Java API（兼容现有公开门面）
               Typed Clients    Dynamic Adapter    Browser
                       └──────────────┬──────────────┘
                               InvocationRequest
                  argv / context / deadline / output policy
                                     │
             ┌───────────────────────┼───────────────────────┐
             │                       │                       │
       Capability Service       Execution Kernel       Diagnostic Projection
       Help / snapshot          原样 argv / 许可       安全元数据 / 关联 ID
       Inputs only              取消 / 有界输出        不默认记录业务载荷
             │                       │
             └─────────────── Transport Capabilities
                                     │
                    ┌────────────────┴─────────────────┐
                    │                                  │
              LocalProcess                    LegacyCollectHttp
         本地全量显式 argv                  仅声明支持的 adapter collect
                    │                                  │
                    └──────────── ExecutionOutcome ─────┘
                                 │
                     Error Decoder / Result Decoder
                     YAML error / JSON / text fallback
                                 │
                     Existing Raw Result + Typed View

Reverse Agent：独立 Worker 生命周期，复用本地 Execution Kernel；不冒充 WS RPC transport。
```

### 4.1 InvocationRequest

语义模型包括：按顺序且不可变的 `argv`、显式的命令类别、单次 ExecutionContext、单调时钟 deadline、OutputPolicy、诊断 correlationId。命令路径和数据值不能由一个“clean strings”函数处理。

Raw 入口的参数值逐项保留，包括空字符串；null token 明确拒绝。结构化请求模型通过 schema 区分 value、optional value、flag 和 negated flag，并保留 `false` 与未提供的区别。重复 options 不能被 Map 悄悄覆盖；旧 Map 构造器继续用于它能够表达的单值子集。

### 4.2 ExecutionContext

单次上下文覆盖客户端默认值，不修改进程全局环境或其它 Client：profile、window、siteSession、keepTab、trace、工作目录与允许的环境覆盖。公共模型保持 Java8 可表达；3.x 可以有内部优化，但不能把 records、virtual threads 等作为共同 API 前置条件。

不可变配置是新 API 的默认；旧 mutable properties 的兼容范围在迁移说明中显式定义，不允许运行中配置变化无声重建全局 Semaphore。

### 4.3 ExecutionOutcome 与兼容结果

至少区分 `SUCCESS / COMMAND_FAILURE / SDK_TIMEOUT / CANCELLED / OUTPUT_LIMIT / TRANSPORT_FAILURE / PROTOCOL_FAILURE / UNSUPPORTED_CAPABILITY`。这些是 SDK 内部规范语义，不冒充上游 error.code。

保留：实际观察的 processExitCode（可未知）、payload error.exitCode、CLI error.code/help/trace、HTTP status、transport origin、部分 stdout/stderr、captured byte counts、是否截断和 elapsed。原有 `OpenCliResult` 与异常层级通过兼容层保留；legacy HTTP 的 0/1 映射若为兼容保留，必须另标 synthetic，不能当作真实远端退出码。

### 4.4 Capability Service

使用上游已证实的 Structured Help 字段：command、usage、commands、positionals、command_options、namespace_options、global_options、required、takes_value、negate、choices。`browser <session> ...` 的占位符参与路径建模，不是应原样传给 CLI 的实际会话值。

快照必须含来源和 hash。发现失败是 UNKNOWN；旧快照是 STALE；已确定当前 transport 不支持才是 UNSUPPORTED。查询描述不等于运行验证，不输出“所有站点可用”。自定义插件、override、external 的来源若无法解析，应记录未解析，不伪造来源标签。

只对信任的、已配置的本地 OpenCLI 执行只读 help；不会为了发现能力安装包、登录、刷新认证或执行发布。Bundled manifest 不是所有本地插件/覆盖的全集。不能从 Help 自动推导完整 Browser 返回 DTO。

### 4.5 Error Decoder

当前 Adapter 错误重点是 **stderr YAML**，字段 `ok:false/error.code/error.message/error.help/error.exitCode`，可能附带 trace 与注释（U04/U05）。成功输出 `-f json` 不改变这个事实。

首先识别受限的 envelope 形状，再解析；支持 JSON envelope，保留纯文本 fallback。YAML 使用安全模式，禁止对象标签、自定义构造、别名扩张，限制大小/深度/文档数。普通 stderr 日志不自动判定为业务错误。解析失败不能覆盖已有非零退出、超时或原始证据。

失败重试不是 decoder 职责。默认不自动重试写命令；只有调用者明确声明可安全重试，并满足同一个 deadline 时才允许策略参与。SECURITY_BLOCK、认证要求等不能触发自动重复写操作。

### 4.6 Browser 结果

保留现有 JsonNode 视图，同时给 state、tab、find、console、network、open/analyze 增加经 fixture 验证的 decoder。不同 leaf 输出可以是数组、对象或文本，不能强行包成同一个已假定 envelope。

保留 unknown fields；schema 变更不会静默变成空成功。外部标识符不得经浮点数损失精度。sitemap 提示暂按可选 metadata 保留；缺失表示 unknown/not-provided，而不是断言站点没有知识。

既有 `OpenCliBrowserSession.close()` 返回 `OpenCliResult`，不能不考虑签名就让它实现 `AutoCloseable.close():void`。本提案不做这种破坏性改造；资源所有权由独立上下文/句柄或显式调用管理，bound 用户标签页不由 SDK 无声关闭。

### 4.7 Local Process

每个客户端或显式共享的 Runtime 只有一个稳定许可管理器；禁止构造新客户端重设其它客户端限制。deadline 从排队开始计时，排队过期不得启动进程。

本提案默认 capture 上限为 stdout 8 MiB、stderr 2 MiB（新设计值，需经评审后实施），超限进入 OUTPUT_LIMIT，返回有限部分结果；不丢弃后假装完整成功。同步采集不是无限 follow 模式；连续输出应使用有界 sink/stream 模式，未提供该模式的 typed API 必须明确拒绝无期限 follow。

取消/超时/输出超限对 SDK 自己拥有的子进程执行有界清理并最终释放许可；不误杀共享 daemon 或用户进程。Java8 与不同 OS 的进程树能力差异必须测试并报告，不能用虚假的统一支持掩盖。

### 4.8 Legacy HTTP

保留 `/collect` 和现有 DTO；建立 ADAPTER_COLLECT 能力边界。不可无损转换的原始 argv、Browser session、root metadata、external passthrough、stdin/streaming 在发送前报 UNSUPPORTED_CAPABILITY。

新建 server-wide `/execute`、v2 argv transport 或 WS RPC 都不在此次实现范围；必须另有 opencli-admin server 契约、版本协商和双方测试。提供显式 header/auth 配置入口时不得把凭据放 URL，不能关闭证书验证；安全策略不等于伪造 server 已支持某种认证协议。

### 4.9 Reverse Worker

状态为 NEW→CONNECTING→REGISTERING→READY→DRAINING→CLOSED；重连只对仍允许重连的活动 Worker 发生。CLOSED 为终态。READY 之前拒绝 collect；最大任务数和队列容量为有限值。

去重作用域是同一中心身份、同一 agent 实例生命周期和 request_id；同 ID 不同载荷拒绝。完成结果使用 TTL/容量上限缓存。连接代际用于保证迟到结果不误投给另一连接；不能承诺重启后 exactly-once，也不自动重放结果未知的写操作。

### 4.10 隐私与诊断

默认日志允许字段为事件类型、correlationId、命令类别、已安全处理的结构性名称、耗时、字节数、状态码；业务参数、stdout/stderr、HTTP body、Authorization、Cookie、URL query 等不进入默认日志。

Raw 结果对已授权调用者仍可访问，日志/export 则用单独投影。不能为满足脱敏而无声改写业务结果。诊断默认只读；任何 restart/update/login/refresh 必须是明确的独立调用。

## 5. 分支策略

提出以 **feature/2.0.x 为行为修复的首个实现目标**，再移植到 1.x 与 3.x；这是本提案决定，不是声称用户之前已决定。理由是保持主干逻辑靠近公共模型，避免将 Java21/Jackson3 特性倒灌 Java8。

允许分歧：JDK、Jackson imports/API、Maven/POM/wrapper、测试运行器以及必要的 OS/process 适配。禁止分歧：argv 值、错误 code/help、deadline、remote 能力拒绝、日志策略、同一 fixture 的业务结论。三分支规范文件应保持相同内容 hash。

不使用整分支 merge 覆盖配置；每个 change 独立移植、构建与测试。升级依赖本身不是能力对齐，不能用“新版本分支”代替语义验证。

## 6. API 迁移策略

保留 raw 和现有 typed 门面，新增能力说明、strict decoder 与不可变请求作为推荐入口。涉及失败行为收紧时提供迁移说明：此前静默丢参数、远端误路由、解析失败被当文本成功等，改为可诊断失败。

旧 mutable/config/Map 接口不直接删除。对于确有兼容需要的 lenient 行为，必须显式命名并记录适用范围，不能绕过安全限制。不得在新版本中无声改变 `OpenCliResult.success` 的意义。

## 7. 明确不做

不实现 Java 浏览器引擎，不移植全部站点代码，不保证所有网站账号随时可用，不新建未经证实的 pipeline CLI，不重命名全部门面，不自动维护本机 SiteMap overlay，不自动安装/升级 CLI 或插件，不把反向 Worker 包装成既有 WS RPC API，不在缺少 server 规范时改 opencli-admin wire format。

## 8. 测试与验收

共享 contract fixtures 是第一资产。TDD 顺序为：先失败测试→最小实现→重构→目标分支验证→其它两线独立验证。

真实 argv 测试使用无外网、无账号的 Java 小进程回显参数；Recording executor 保留为快速单元测试但不能作为最终保真证明。Process 使用可控子进程测试取消、超时和大输出。HTTP/WS 使用可控本地 fixture server 测试请求形状、malformed response、断线与重复消息。上游 smoke 只执行 version/help/离线 fixture，不用用户账号执行 publish/delete/send。

构建需分别记录 Java8/17/21、各自 Maven wrapper、测试数、跳过数、exit code、HEAD 和结果 artifact。零测试、未触发 CI、尚在运行都不是通过。

发布前必须关闭相关 P0，以及完成规范 strict 校验、三线 contract fixture 等价、日志泄露负例、针对锁定 OpenCLI 的 schema drift 检查。文档格式通过不等于实现通过。
