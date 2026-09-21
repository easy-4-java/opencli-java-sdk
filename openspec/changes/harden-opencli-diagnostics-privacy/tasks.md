# C09 Tasks · 默认日志与异常的隐私边界

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-diagnostics-privacy/spec.md)。

## 1. 泄露负例

- [ ] 1.1 对Executor、AdapterChannel、HTTP、WS、JSON/YAML parser所有日志/异常点建立canary矩阵。
- [ ] 1.2 新增完整日志捕获与nested cause测试，先证明当前泄露可被测试检测。

## 2. 安全投影

- [ ] 2.1 将commandLine/args/bodyPreview/remote error输出改成允许表元数据与correlationId。
- [ ] 2.2 实现安全异常摘要和cause处理，避免logger输出provider原始对象。
- [ ] 2.3 保留raw业务结果与默认关闭的HTTPdebugcapture，新增显式有限export策略。

## 3. 验证

- [ ] 3.1 覆盖DEBUG、timeout、malformedresponse、断线和超限路径，任何canary出现均失败。
- [ ] 3.2 验证raw结果内容未被脱敏逻辑无声修改。
- [ ] 3.3 三线执行相同隐私矩阵并记录证据；文档标明SDK与应用日志责任边界。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。
