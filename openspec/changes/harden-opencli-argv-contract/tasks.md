# C01 Tasks · 参数保真与统一调用契约

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-argv-contract/spec.md)。

## 1. 失败测试

- [ ] 1.1 在现有 core/support 测试区新增 Java argv echo fixture，覆盖空值、空白、Unicode、换行、-- 和重复 flag。
- [ ] 1.2 先在实现前运行新增用例，保存真实失败输出；禁止只断言命令前缀。

## 2. 最小实现

- [ ] 2.1 统一 Executor/AdapterChannel/参数 helpers 的值保真路径，并对输入列表做快照。
- [ ] 2.2 为 null/标识符错误建立不包含业务值的校验；增量支持有序 option occurrences 和 false 语义。

## 3. 迁移与验证

- [ ] 3.1 补 List/varargs/typed/request 等价与用户列表不被修改的测试。
- [ ] 3.2 检查 Windows launcher 与引号边界，未测试的平台明确保留未验证状态。
- [ ] 3.3 分别在三条 feature 线运行共享 vectors 和现有测试，记录允许差异。
- [ ] 3.4 评审迁移说明与完整 diff；需求通过后再申请 archive，不在文档阶段勾选。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。
