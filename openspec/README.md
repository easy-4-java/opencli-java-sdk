# OpenSpec 变更路线图

> DRAFT / REVIEW_REQUIRED。10个Change都是待实现规格，不代表已完成能力。详见 [架构](../docs/architecture/opencli-java-sdk-target-architecture.md) 与 [复核](../docs/architecture/opencli-java-sdk-reverification.md)。

## Change索引

| 编号 | Change | 优先级 | 硬实现前置 |
|---|---|---|---|
| C01 | [harden-opencli-argv-contract](changes/harden-opencli-argv-contract/proposal.md) | P0 | 无 |
| C02 | [harden-opencli-process-lifecycle](changes/harden-opencli-process-lifecycle/proposal.md) | P0 | C01 |
| C03 | [preserve-opencli-error-envelope](changes/preserve-opencli-error-envelope/proposal.md) | P0 | C02 |
| C04 | [harden-opencli-remote-collect](changes/harden-opencli-remote-collect/proposal.md) | P0 | C01, C02, C03 |
| C05 | [harden-opencli-reverse-agent](changes/harden-opencli-reverse-agent/proposal.md) | P0 | C02, C03 |
| C06 | [add-opencli-capability-discovery](changes/add-opencli-capability-discovery/proposal.md) | P1 | C01, C02, C03, C04 |
| C07 | [add-opencli-browser-result-models](changes/add-opencli-browser-result-models/proposal.md) | P1 | C01, C03, C06 |
| C08 | [add-opencli-context-and-diagnostics](changes/add-opencli-context-and-diagnostics/proposal.md) | P1 | C04, C06, C07 |
| C09 | [harden-opencli-diagnostics-privacy](changes/harden-opencli-diagnostics-privacy/proposal.md) | P0 | 无 |
| C10 | [enforce-opencli-branch-contract-parity](changes/enforce-opencli-branch-contract-parity/proposal.md) | P0/P1 | 无 |

## 依赖与实施顺序

先评审本包；建议 C10 的基础阶段建立共同fixture/来源lock/分支报告格式。该基础可与C01、C09失败测试先行，不要求提前关闭C10。

主执行顺序为：C01 → C02 → C03；然后C04与C05可在满足各自前置后推进；C09跨上述路径尽早落实。之后C06 → C07 → C08。最后关闭C10集成门禁。并行只适用于互不修改同一实现面的任务，不假定存在并行代理或自动执行。

C10没有循环依赖：其基础工作可先行，而最终关闭条件是选定C01–C09全部实现且三线证据齐备。

## 首个实现目标与版本线

提议以feature/2.0.x为首个行为修复目标，然后逐Change移植1.x/3.x；这是一项待评审的本次设计选择。三线规范与fixture保持一致，Java/Jackson/Maven差异明确列入允许表。

## 规范格式与状态

每个目录都有proposal.md、design.md、tasks.md、.openspec.yaml和specs/<capability>/spec.md。Requirements使用SHALL，Scenario使用WHEN/THEN，并设置稳定ID。因为是首次规范化，全部使用ADDED Requirements；已存在的API仍须在proposal中明确。

根`openspec/specs/`暂不放已生效规范，所有任务未勾选。规范评审、TDD实现、三线验证、官方strict校验和archive是不同步骤。

## 暂缓或不纳入

完整Sitemap文件/overlay管理、Java Pipeline/Node bridge、上千Adapter全手写DTO、任意远程argv的虚构/execute端点、反向Worker冒充应用端WS RPC均不纳入本轮强制实现。新增需求必须另立证据和变更。

## 验收导航

- [需求到测试的计划矩阵](../docs/architecture/opencli-java-sdk-acceptance-matrix.csv)：每条Requirement有正负Scenario，状态PLANNED。
- [合成契约向量](../docs/architecture/opencli-java-sdk-contract-vectors.json)：用于指导失败测试，不是已运行capture。
- [文档验证报告](../docs/architecture/opencli-java-sdk-validation.md)：区分自检与官方CLI/代码测试。

## 官方CLI验证（待具备环境后执行）

```bash
openspec --version
openspec list --json
openspec validate --all --strict --no-interactive
```

逐Change执行同名validate并保留退出结果。网络/安装失败时如实标NOT_RUN，不得用手工检查伪装成官方strict通过。
