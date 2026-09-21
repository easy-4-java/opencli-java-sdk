# C07 Tasks · Browser 领域结果与严格解析

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-browser-result-models/spec.md)。

## 1. 输出证据

- [ ] 1.1 固定第一批leaf的真实输出fixtures、版本/平台/hash；无样本者在支持表标pending。
- [ ] 1.2 先补 malformed/truncated/empty/unknownfields/largeID/AX 的失败测试。

## 2. 模型与decoder

- [ ] 2.1 实现Java8公共模型与各Jackson分支兼容decoder，避免以double中转ID。
- [ ] 2.2 保留raw/JsonNode与旧close返回签名，新增strict访问而非破坏旧API。
- [ ] 2.3 建立每leaf字段约束和parse-state，未验证shape不自动转成空成功。

## 3. 集成验收

- [ ] 3.1 与C03错误/trace、C02截断、C08上下文保持一致，禁止自动读取sitemap路径。
- [ ] 3.2 在三线共享fixtures上验证字段与失败语义等价。
- [ ] 3.3 生成实际typed支持表，区分已验证和仅raw支持，补迁移示例。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。
