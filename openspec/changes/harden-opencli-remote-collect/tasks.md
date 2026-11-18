# C04 Tasks · Legacy HTTP collect 边界与保真

> 勾选项具备对应的实现或执行证据。待验收项继续保留。

关联规范：[spec](specs/opencli-remote-collect/spec.md)。

## 1. 服务端契约与失败测试

- [x] 1.1 固定可控的 legacy `POST /collect` fixture，记录本地桩与上游 CLI 的范围差异。见下方 OC-REMOTE-004 证据。
- [x] 1.2 验证 Browser、根命令、外部透传和旧 DTO 无法保真的输入均在发送前拒绝。见下方 OC-REMOTE-001/002 证据。
- [ ] 1.3 编写 success=false、503、malformed、null、超大body与超时结果未知用例。

## 2. 最小适配修复

- [ ] 2.1 引入显式 legacy capability guard 和保真转换子集，避免无条件解析任意 argv。
- [ ] 2.2 区分 HTTP/协议/命令失败、synthetic exit 与未观察的退出码。
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
