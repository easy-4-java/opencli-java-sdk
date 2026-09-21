# C04 Tasks · Legacy HTTP collect 边界与保真

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-remote-collect/spec.md)。

## 1. 服务端契约与失败测试

- [ ] 1.1 固定 opencli-admin server 协议版本或可控 fixture，标明它不是 jackwener/OpenCLI 的官方 RPC。
- [ ] 1.2 对 Browser/root/external/重复flag/负数/--/空值编写“拒绝且零请求”的测试。
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
