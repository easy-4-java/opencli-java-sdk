# C04 Tasks · Legacy HTTP collect 边界与保真

> 勾选项具备对应的实现或执行证据。待验收项继续保留。

关联规范：[spec](specs/opencli-remote-collect/spec.md)。

## 1. 服务端契约与失败测试

- [x] 1.1 固定可控的 legacy `POST /collect` fixture，记录本地桩与上游 CLI 的范围差异。见下方 OC-REMOTE-004 证据。
- [x] 1.2 验证 Browser、根命令、外部透传和旧 DTO 无法保真的输入均在发送前拒绝。见下方 OC-REMOTE-001/002 证据。
- [x] 1.3 验证业务失败、HTTP 503、畸形或 null 响应、响应超限及超时后远端结果未知。见下方 OC-REMOTE-004/005 证据。

## 2. 最小适配修复

- [ ] 2.1 引入显式 legacy capability guard 和保真转换子集，避免无条件解析任意 argv。
- [x] 2.2 区分 HTTP、协议与命令失败，保留兼容退出码及其来源。见 OC-REMOTE-003/004 验证。
- [ ] 2.3 使 body/timeout 有界并实现可测试的显式 header 配置，保持 TLS。

## 3. 三线与安全

- [ ] 3.1 对照 C09 测试凭据、URL query、body preview 和异常 cause 的默认脱敏边界。
- [ ] 3.2 在三分支分别跑本地 HTTP fixture；记录旧 API 的行为收紧与迁移例子。
- [ ] 3.3 未取得服务端契约的扩展仍标记 unsupported，不借此宣称远程全量兼容。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。


## OC-REMOTE-004 响应校验

`OpenCliRemoteAgentHttpClientTest.JsonCollectStub` 通过本机 HTTP 服务器提供可控的 legacy `POST /collect` 响应。证据只覆盖 SDK 到此测试桩的 HTTP 交互。jackwener/OpenCLI 提供本地 CLI 接口，真实 opencli-admin 部署仍需验收。

JDK 17 基线 `5ac4bb1` 的 20 项用例实际执行，7 项失败。缺少或显式设为 null 的 `success` 被当成业务失败。字符串和数字布尔值、数字 `error`、重复状态字段及拼接文档曾被当成成功。修正后，具备布尔状态字段的单个 JSON 对象才会继续映射结果，无效响应通过 `OpenCliRemoteProtocolException` 保留类别和相关 ID。现有异常父类保持兼容。合法的成功响应允许省略或置空 `items`，空结果仍为 `[]`。

定向测试命令为 `-Dtest=OpenCliRemoteAgentHttpClientTest test`。使用各维护线的 Maven 和 JDK，报告位于 `target/surefire-reports`。Body 大小限制、总超时和退出码来源仍由其他任务验收。


## OC-REMOTE-001/002 发请求前校验

基线 `cf9360e` 的集成测试中，16 组输入有 15 组未按要求在 HTTP 前拒绝。既有 SDK 适配器名可能和外部透传调用重名，额外测试复现了该路径；改为仅本地执行后，HTTP 桩记录为零请求。正常 npm search 调用仍发送一次，并检查实际 JSON 的 site、command、positional_args 和 args。公开的低层 `collect(OpenCliCollectRequest)` 继续接收应用已经验证过的服务端 DTO。

本轮证据来自本地桩和各维护线全量验证，当前 SDK 适配器清单用于限制 argv 路由。各 Agent 部署的实际命令与选项契约仍需验证。


## OC-REMOTE-005 响应体预算

JDK 17 基线在 42 项 HTTP 用例中有 4 项失败，覆盖多字节响应超出一字节、HTTP 200 和 503 的分块响应，以及 gzip 解压超限。修改复用现有 Unirest 的原始响应流，从解压后的流读取至上限。读取超限检测只需额外一个字节，超限后由 Unirest 释放响应连接。既有整包读取 API 无法在流尚未结束时执行这一限制。分块桩在收到结果前保持响应开启，验证客户端不等待 EOF。

默认上限为 10 MiB，可通过 `maxRemoteResponseBytes` 配置正数。异常保留 HTTP 状态、上限和相关 ID。启用原文捕获的客户端也无法取得超限内容。补充验证恰好满足预算、声明字符集、无效配置零请求以及超限后同一客户端继续执行。总超时、结果不确定性和认证 header 仍待相应场景验收。


## OC-REMOTE-005 持续分块与总预算

JDK 17 基线中，chunked 响应每 125 ms 持续发送数据，900 ms 的原套接字超时未能终止调用，测试在 3 秒断言窗口失败。使用现有 Apache HttpClient 的可取消请求后，连接池等待、建连、HTTP 头及有界读取共享一个单调时钟预算。超时只中止当前请求，已提交传输的调用返回远端结果未知，无自动重试。原始预算耗尽且尚未提交时返回未发送。当前响应池可继续处理后续调用，并保留 JVM 信任配置。另有 307 回归验证不会转发采集请求。

本轮验证的是 SDK 侧 HTTP 传输与回环 HTTP 桩。真实部署、系统 DNS 解析器或本地解析处理的执行时延，以及认证 header 仍需独立验收。


## OC-REMOTE-003/004 状态与退出码来源

对基线加入结果证据断言后，HTTP 200 业务失败、HTTP 503、缺省 `items` 和十种畸形响应共 13 项未能提供所需证据。修改后，`getRemoteExecutionDetails()` 保留 HTTP 状态、Agent 业务状态、失败层级和 `items` 来源。兼容退出值标记为 `SYNTHETIC_COMPATIBILITY`，观测到的远端退出码为空。HTTP 或协议失败快照保留真实状态且退出值为空，按原文捕获开关提供通过字节上限检查的完整响应。超限和超时沿用原有异常及保留策略。

验证覆盖 503 的 HTML 响应、可选原文与安全诊断、合法业务失败和协议失败。显式诊断导出先输出退出码来源，再输出兼容退出值，避免截断产生缺少来源的数字。
