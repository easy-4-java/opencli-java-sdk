# C09 Tasks · 默认日志与异常的隐私边界

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-diagnostics-privacy/spec.md)。

## 1. 泄露负例

- [x] 1.1 对Executor、AdapterChannel、HTTP、WS、JSON/YAML parser所有日志/异常点建立canary矩阵。见本页失败场景与 24 个日志调用点的证据。
- [x] 1.2 新增完整日志捕获与nested cause测试，先证明当前泄露可被测试检测。

## 2. 安全投影

- [x] 2.1 将commandLine/args/bodyPreview/remote error输出改成允许表元数据与correlationId。见本文日志调用点核对。
- [x] 2.2 实现安全异常摘要和cause处理，避免logger输出provider原始对象。见本页的异常链、HTTP 清理及响应映射、WebSocket 故障注入回归。
- [ ] 2.3 保留raw业务结果与默认关闭的HTTPdebugcapture，新增显式有限export策略。

## 3. 验证

- [x] 3.1 覆盖DEBUG、timeout、malformedresponse、断线和超限路径，任何canary出现均失败。
- [ ] 3.2 验证raw结果内容未被脱敏逻辑无声修改。
- [x] 3.3 三线执行相同隐私矩阵并记录证据；文档标明SDK与应用日志责任边界。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## 2026-11-19 C09 默认异常链与三线隐私矩阵

JDK 17 两项定向红灯实际退出 1。选项对象的 `toString()` 抛出带嵌套 cause 和 suppressed 项的异常，栈文本泄露测试密钥。真实缺失可执行文件的低层 result handler 异常链泄露路径。修复后，值转换报告固定字段诊断，result handler 保留安全摘要。显式 `RunSession.getIoFailure()` 仍返回原始低层诊断。两项回归都扫描完整栈文本，保留原始数据的测试同时验证权限边界。

| 场景 | 现有三线回归 |
| --- | --- |
| Adapter DEBUG、本地非零退出、超时、超限、原始 stdout/stderr | `OpenCliProcessPrivacyTest` |
| 参数对象的嵌套 cause/suppressed、底层原生路径 | `OpenCliArgSupportTest`、`SubprocessExecutionSupportTest` |
| HTTP 503、凭据 URL、畸形 JSON、原文捕获开关 | `OpenCliRemoteAgentHttpClientTest` |
| WebSocket 断线和投递异常、配置 URL、日志测试密钥 | `OpenCliWsReverseAgentSessionTest`、`OpenCliWsReverseAgentClientTest` |
| JSON 解析 DEBUG、默认不写入的有界导出 | `OpenCliStdoutJsonTest`、`OpenCliDiagnosticExportTest` |

JDK 21 使用仓库 Maven Wrapper 执行 `-B -ntp -o clean verify`，2,133 项通过，0 失败、0 错误、0 跳过。中英文 README 记录应用显式访问原始低层诊断的责任。畸形 YAML 的验证见 C03 的隐私回归及下文。


## 本地失败诊断关联

OC-PRIV-001/004。OpenCliProcessPrivacyTest 的新用例在 Java 17 基线复现缺少 correlationId，退出 1。现在本地进程失败时由 SDK 生成随机 UUID，在 DEBUG 日志及返回的异常消息中使用同一值。日志只记录终止原因、退出码和捕获字节数。测试扫描捕获的日志和完整异常栈，同时通过 partialResult 校验 stderr 原值。C03 的 OpenCliErrorEnvelopeTest 覆盖含敏感标记的畸形 YAML，并确认解析器原文不进入默认诊断。

Java 8、17、21 分别用各自 Wrapper 运行 OpenCliProcessPrivacyTest、OpenCliErrorEnvelopeTest 和 OpenCliProcessContractTest，每条线执行 31 项，零失败、零错误、零跳过。报告位于 target/surefire-reports。Java 21 的先前运行曾出现继承管道用例的 JUnit 临时目录清理竞态，本次重跑通过。该竞态需要独立处理。

## SDK 配置对象的默认诊断

OC-PRIV-001/004。Java 17 基线 `58f675a` 的两项回归均失败。输出 `OpenCliProperties` 或 `OpenCliWsAgentConnectionProperties` 时，Lombok 的默认字符串表示包含测试标记。改为显式允许表后，默认字符串只包含受控枚举、开关、字节预算和超时、并发等数值。CDP 地址、本地路径、启动参数、环境变量、WebSocket 地址和节点标签仍可通过各自的配置 API 读取和使用。

实现分别基于 Java 8 `afc6b33`、Java 17 `58f675a`、Java 21 `5cfa937`。三线使用各自 Wrapper 执行 `./mvnw -B -ntp -o clean verify`，分别通过 2,242、2,243、2,242 项。失败、错误、跳过均为 0。两项新增测试记录在 `OpenCliPropertiesTest` 和 `OpenCliWsReverseAgentClientTest` 的 Surefire 报告中。随后同步的 Agent TLS 证据提交只触及 OpenSpec 材料。

## 请求和请求构建器的默认诊断

OC-PRIV-001/003/004。Java 17 基线 `c6dc643` 的两项回归均失败。HTTP 收集 DTO 和生成的构建器会将命令、选项、位置参数和 CDP 地址拼进字符串，结构化本地请求构建器也会拼入命令及参数。三个默认字符串只保留类型名称。请求值仍由原访问器和协议序列化读取，结构化命令生成相同的字面 argv。

Java 8 `15ef02a`、Java 17 `c6dc643`、Java 21 `d0f0205` 为回归前基线。三线各自用 Wrapper 执行 `./mvnw -B -ntp -o clean verify`，分别通过 2,244、2,245、2,244 项，零失败、零错误、零跳过。新增用例见 `OpenCliCollectRequestTest` 和 `OpenCliAdapterCommandRequestTest` 的 Surefire XML。全量回归同时覆盖 HTTP 线上字段和本地子进程调用。


## WebSocket 连接关联与日志调用点核对

OC-PRIV-001/004。Java 17 基线 `0b7091c` 的 TLS 重连用例中缺少连接关联 ID。实现为每次反向连接尝试生成独立 UUID，同一连接的建立、校验失败、回调、收集失败、投递失败和关闭失败日志复用该值。连接协议、业务结果和去重键各自沿用原值。TLS 回归注入包含测试标记的投递失败，验证原连接与失败日志共享 ID，重连使用新 ID，并保留显式重试和结果去重断言。

Java 8 `cd1ceac`、Java 17 `0b7091c`、Java 21 `5bee58b` 是本轮修改前基线。三线各使用 Wrapper 执行 `./mvnw -B -ntp -o clean verify`，分别通过 2,244、2,245、2,244 项，零失败、零错误、零跳过。Java 17 修改前实际执行两个 TLS 参数用例，其中一个因缺少关联 ID 失败，修改后三线全量均通过。

| SDK 发出日志的位置 | 调用点数 | 允许的动态值 | 运行证据 |
| --- | ---: | --- | --- |
| 本地执行器、适配器、浏览器命令入口 | 3 | 随机关联 ID、终止原因、退出码、字节数或参数个数 | `OpenCliProcessPrivacyTest` 与全量浏览器调用 |
| JSON 输出检测、远端 argv 转换 | 2 | 长度或参数个数 | `OpenCliStdoutJsonTest`、`OpenCliArgvToCollectParserTest` |
| HTTP 客户端 | 10 | 随机关联 ID、HTTP 状态、长度 | `OpenCliRemoteAgentHttpClientTest`，七个调用点实际执行 |
| 反向 WebSocket 客户端 | 9 | 连接 UUID 或重连次数 | `OpenCliWsReverseAgentSessionTest`、`OpenCliWsReverseAgentClientTest`，七个调用点实际执行 |

每线当前 JaCoCo 报告均覆盖 24 个 SDK 日志调用点中的 19 个。仍缺独立驱动 HTTP 请求序列化失败、外层 JSON 映射失败、HTTP transport 关闭失败、WebSocket onError 及 WebSocket 关闭抛异常的动态证据。源码核对确认这些日志也使用固定消息或 SDK 生成的 ID，JaCoCo 缺口由 1.1 和 2.2 继续跟踪。该核对限于 SDK 自身的日志调用与实测异常。

## HTTP 资源关闭异常的隐私回归

OC-PRIV-002/006。Java 17 基线 `ddfa6c8` 的四个定向用例有两个失败。无效响应的协议异常附带了原始 HTTP 响应关闭异常，公开异常栈包含测试标记。连接池关闭的运行时异常也直接暴露了标记。响应清理现在仅传播固定消息，协议失败保留主要类别和 HTTP 200。单独发生的响应关闭 I/O 故障保留传输失败及已观察到的状态，关闭超时沿用超时分类。连接池关闭失败使用固定日志。请求序列化的独立回归确认测试标记未出现在日志或异常栈，回环服务接收请求数为 0。

三线分别以 Java 8 `d5799ac`、Java 17 `ddfa6c8`、Java 21 `0b4b0c8` 为基线修改，用各自 Wrapper 执行 `./mvnw -B -ntp -o clean verify`。分别通过 2,248、2,249、2,248 项，零失败、零错误、零跳过。响应和连接池的关闭异常使用注入的 Apache HTTP provider 测试，协议解析和序列化回归分别使用 JSON 夹具及回环服务。三份 JaCoCo 均命中 24 个日志调用点中的 21 个。HTTP 外层响应映射失败、WebSocket onError 和 WebSocket 关闭抛异常仍待动态证据。


## HTTP 响应映射与 WebSocket provider 异常

OC-PRIV-001/002/003/006。HTTP 夹具返回合法响应并使用 Jackson 的实际写入深度约束触发 items 映射失败。Java 17 在原分类逻辑下抛出没有状态证据的通用异常。修复后保留 HTTP 200、`PROTOCOL_FAILURE`、显式捕获的原响应及空退出码。公开异常栈和 SDK 日志只使用固定诊断和关联 ID。数组和其他有效 `items` 使用同一 Jackson writer，普通协议回归仍通过。

真实回环 WebSocket 连接收到注入的 provider error 回调后，旧连接退出，新连接完成注册与 ping/pong。错误日志保留旧连接关联 ID，隐藏 provider 测试标记。另一用例在无网络连接的 Java-WebSocket 1.6.0 夹具上注入底层 close 异常。SDK 清空接单连接，只调用一次关闭，关闭后拒绝重启，DEBUG 日志保留关联 ID。该用例验证 SDK 的错误处理，实际套接字关闭由其他回环测试验证。

Java 8 `f4c6677`、Java 17 `6894071`、Java 21 `a0ba264` 是修改前基线。三线各使用 Wrapper 执行 `./mvnw -B -ntp -o clean verify`，分别通过 2,251、2,252、2,251 项，零失败、零错误、零跳过。三份报告均执行了新增的三项回归，JaCoCo 显示每线 24 个 SDK 日志调用点均被执行。此前各节保留供应商原文、异常链、日志与应用显式读取原始结果的证据边界。
