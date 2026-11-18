# C09 Tasks · 默认日志与异常的隐私边界

> 勾选项已有实现和对应测试证据。文档和格式检查单独记录。

关联规范：[spec](specs/opencli-diagnostics-privacy/spec.md)。

## 1. 泄露负例

- [ ] 1.1 对Executor、AdapterChannel、HTTP、WS、JSON/YAML parser所有日志/异常点建立canary矩阵。
- [ ] 1.2 新增完整日志捕获与nested cause测试，先证明当前泄露可被测试检测。

## 2. 安全投影

- [ ] 2.1 将commandLine/args/bodyPreview/remote error输出改成允许表元数据与correlationId。
- [ ] 2.2 实现安全异常摘要和cause处理，避免logger输出provider原始对象。
- [x] 2.3 保留raw业务结果与默认关闭的HTTPdebugcapture，新增显式有限export策略。

## 3. 验证

- [ ] 3.1 覆盖DEBUG、timeout、malformedresponse、断线和超限路径，任何canary出现均失败。
- [x] 3.2 验证raw结果内容未被脱敏逻辑无声修改。
- [ ] 3.3 三线执行相同隐私矩阵并记录证据；文档标明SDK与应用日志责任边界。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## 2.3 与 3.2 验证记录

- OC-PRIV-003 与 OC-PRIV-005。公开入口为 `OpenCliDiagnosticExport.export(result, maxBytes)`。SDK 在内存中生成允许表投影，保留原始结果的内容，SDK 自身的导出保留时间为零。
- `OpenCliDiagnosticExportTest` 覆盖原值保留、全量与截断、恰好满足预算、预算少一个字节、非法预算和缺失字段。API 缺失时验证失败，边界回归随后复现截断标记不一致。修复后全部通过。现有 HTTP 测试也验证原报文的显式捕获和默认关闭。
- 1.0.x 功能分支提交 `295f1d32d2b00f7f3d1ae74eb4f95d792b309d9c`，JDK 8，`mvn -B --no-transfer-progress clean verify`，2016 个测试。
- 2.0.x 功能分支实现提交 `72abff8d0d09918922886aef364a3b85c14f4d09`，JDK 17，`mvn -B --no-transfer-progress verify`，2087 个测试。
- 3.0.x 功能分支提交 `6fa874a3c811733126350ca75beb750f78ba8956`，JDK 21，`bash ./mvnw -B --no-transfer-progress clean verify`，2016 个测试。
- 三线本地构建退出码均为 0，失败、错误和跳过均为 0。报告位于各构建目录的 `target/surefire-reports`。其余场景和跨平台验收仍由相应任务跟踪。
