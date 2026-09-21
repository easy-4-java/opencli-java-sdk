# C05 Tasks · 反向 Worker 状态机、背压与去重

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-reverse-agent/spec.md)。

## 1. Worker fixture

- [ ] 1.1 新增可控本地 WS server，覆盖未注册 collect、超大/空 ID、队列满与重复消息。
- [ ] 1.2 加入断线写任务、代际结果路由、close竞态和重启边界的失败测试。

## 2. 治理实现

- [ ] 2.1 建立显式状态机及 READY gate，接收线程仅做有界解析与准入。
- [ ] 2.2 用有限执行器替换 cached pool，加入容量校验和兼容 BUSY result。
- [ ] 2.3 实现 payload digest、在途记录与有界完成缓存；同 ID 不同载荷拒绝。
- [ ] 2.4 实现连接代际与有界 drain/cancel，CLOSED 终态。

## 3. 兼容与验收

- [ ] 3.1 证明重连不自动重复写入，完成发送失败不被误标为执行失败。
- [ ] 3.2 验证现有消息结构在 legacy fixture 中不变，安全日志符合 C09。
- [ ] 3.3 分别在三线运行 WS压力/断线fixture，记录执行次数、线程数与终结资源结果。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。
