# 文档验证报告

> 日期：2026-09-21。结果范围：**提案文档结构与交付物完整性**，不是SDK实现验证。

> 本报告保留规范生成阶段的验证范围；提交阶段的最新基线见 [提交前复核补记](opencli-java-sdk-publish-check.md)。提交前已重新运行同一检查器，JSON报告为本次结果。原始阶段 NOT_RUN 的 GitHub 状态不得被误读为后续推送状态；远端结果须另行回读。

## 1. 自定义结构检查：通过

实测检查得到：10个Change、59条Requirement、118个Scenario、84项待实施任务；已勾选任务为0。每个Change具备proposal、design、tasks、元数据和capability spec。Requirement ID不重复；Scenario包含WHEN/THEN；依赖图无环；验收CSV逐Scenario对应；JSON/YAML能够解析；相对文件链接可解析。

执行命令（在本包根目录）：

```bash
python docs/architecture/tools/validate-spec-pack.py --root . --self-test \
  --output docs/architecture/opencli-java-sdk-validation.json
```

当前环境使用Python和PyYAML。检查器适用于Python 3.9+；缺少PyYAML时会明确报告YAML检查不完整，不要求用它替代OpenSpec CLI。

结果见 [JSON报告](opencli-java-sdk-validation.json)，源码见 [检查器](tools/validate-spec-pack.py)。自检器的7个负例均被检测：缺规范关键词、错误Scenario标题层级、缺WHEN、缺THEN、提前勾选任务、坏链接、循环依赖。它们是**检查器自身的负例**，不是84项SDK实现任务的测试结果。

## 2. 补丁与归档校验

交付补丁只包含本包的新增文件，不修改Java源码、现有测试、POM或workflow。生成时在独立临时目录执行 `git diff --cached --check`，并在另一个干净临时目录执行 `git apply --check` 与实际apply，再逐文件比对内容。

此结果只证明补丁格式及新文件内容可以应用到空白目标；**没有在用户真实checkout上验证合并**。目标若已有同名文件，必须先本地check并人工合并，不能强制覆盖。

ZIP进行CRC检查和逐项路径/内容检查；随包 `SHA256SUMS` 校验所有其它文件。补丁/ZIP的实际最终验证记录在独立交付收据中，不把生成文件当作GitHub commit。

## 3. 未执行的验证

| 验证 | 状态 | 原因与边界 |
|---|---|---|
| 官方OpenSpec CLI strict | NOT_RUN_ENVIRONMENT | 工具未安装；npm DNS返回EAI_AGAIN，无法完成安装；自检不能替代官方CLI |
| CodeGraph三分支索引/调用图 | NOT_RUN_ENVIRONMENT | 本轮没有可用工具或完整本地checkout；不是“等价CodeGraph已完成” |
| Maven三分支构建/单测 | NOT_RUN | 本轮未取得完整本地代码及工具链运行证据 |
| 实际OpenCLI version/help/命令smoke | NOT_RUN | 本轮核对的是GitHub固定源码，不是安装的CLI运行输出 |
| HTTP/WS真实fixture回归 | PLANNED_NOT_RUN | 已写入OpenSpec Scenario与tasks，尚未实现运行 |
| GitHub CI | NOT_RUN | 本轮没有提交/推送，也没有触发或观察新的CI |
| 用户Mac工作区落盘 | NOT_PERFORMED | 本轮WebCodex工具发现未接通；文件生成在当前对话工作容器 |
| 目标仓库git apply检查 | NOT_RUN | 仅独立空目录验证，目标同名文件冲突尚待现场check |

## 4. 复核结论的强度

参数裁剪、静态Semaphore替换、无界buffer、legacy转换结构、日志输出、YAML错误渲染等是已读取源码中的观察。并发峰值、子进程残留时间、远端副作用与网站真实成功率是尚未实测的风险或待验收行为。

全部规范状态保持DRAFT / REVIEW_REQUIRED，全部实现任务保持未勾选。正式关闭需要需求评审、真实失败→通过测试、三条版本线证据以及官方strict校验。
