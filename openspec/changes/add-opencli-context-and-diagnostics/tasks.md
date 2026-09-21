# C08 Tasks · 隔离执行上下文与只读诊断

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-context-diagnostics/spec.md)。

## 1. 上下文契约

- [ ] 1.1 核实锁定版本profile与Browser公共options的scope和真实输出fixture。
- [ ] 1.2 编写并发context隔离、false覆盖、scope冲突与诊断UNKNOWN的失败测试。

## 2. 增量API

- [ ] 2.1 实现不可变context与优先级合并，复用原meta/auth/skills入口。
- [ ] 2.2 实现per-check只读诊断，审计每个probe副作用；不能确认时保留unknown。
- [ ] 2.3 保留sitemap raw/optional metadata，不自动访问overlay或trace文件。

## 3. 边界评审

- [ ] 3.1 证明诊断不会执行login/refresh/restart/install/profile use等变更操作。
- [ ] 3.2 更新DEFERRED清单，移除未经证实的pipelineCLI/单进程承诺。
- [ ] 3.3 三线验证上下文与诊断状态一致，并在指南中给出明确的只读使用示例。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。
