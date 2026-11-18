# C10 Tasks · 共享契约测试、分支差异与上游锁定

> 已验收的准备任务见下方证据。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-branch-contract-parity/spec.md)。

## 1. 基础阶段可先行

- [ ] 1.1 记录三个精确HEAD和允许差异矩阵，确定first-implementation分支经评审认可。
- [ ] 1.2 建立共享contractfixture目录、来源lock与测试runner，复用现有coverage而不删除其它测试。
- [ ] 1.3 定义完整argv/协议/typed/live分层报告，加入0测试和missingreport失败规则。

## 2. 上游与文档

- [ ] 2.1 采集锁定上游的实际help/manifest/output，保存hash与范围，移除对作者绝对路径的依赖。
- [x] 2.2 修正各分支README/POMdescription/Gradle示例，检查Java/Jackson/Maven实际声明。
- [x] 2.3 配置三线正确wrapper/JDK的独立构建与报告，不通过修改coverage阈值绕开行为问题。

## 3. 最终集成门禁

- [ ] 3.1 在C01–C09选定实现完成后，分别在三线执行全部对应正负fixtures，记录HEAD与真实退出结果。
- [ ] 3.2 运行官方OpenSpec逐Change与all strict校验，保存CLI版本和报告；环境失败保持NOT_RUN。
- [ ] 3.3 核对Requirement到测试/commit的闭环并进行独立代码评审，才允许勾选和archive。
- [ ] 3.4 提交/推送/PR/发布仅在用户明确授权时执行；观察实际CI，不承诺未发生的远端结果。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

### 2.2 与 2.3 的三线验收

OC-PARITY-005：以表中检出的三条线为准，逐条核对中英文 README 的 Maven 和 Gradle 坐标、POM 描述与 Java/Jackson 声明，以及 Wrapper 的分发版本。三个版本的坐标均为 `io.github.easy4j:opencli-java-sdk`，分别使用 `1.0.x.20260630-SNAPSHOT`、`2.0.x.20260630-SNAPSHOT` 和 `3.0.x.20260630-SNAPSHOT`。

OC-PARITY-004：维护线原 CI 将 JaCoCo 参数丢在 Surefire JVM 之外，构建成功时仍缺执行数据。修复后的各线 CI 使用其 Wrapper 执行 `./mvnw -B --no-transfer-progress clean verify`。三次退出码均为 0，Surefire XML 的失败、错误、跳过数均为 0，且均上传了 `surefire-reports` 和 `jacoco-coverage`。三条线保留 90% 行覆盖率告警阈值，并要求上传 JaCoCo 报告。

| JDK | Wrapper Maven | Jackson | 构建提交 | 测试数 | 行覆盖率 | GitHub CI |
| --- | --- | --- | --- | ---: | ---: | --- |
| 8 | 3.9.16 | 2.18.9 | `aaa295fc6e65708554ab7bb55cf7c05fb990f6ae` | 2144 | 3043/3303（92.13%） | [44356116353](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44356116353) |
| 17 | 3.9.16 | 2.22.1 | `cf9360e99647c935d7ee10c87a391ed602647682` | 2145 | 3030/3286（92.21%） | [44355648596](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44355648596) |
| 21 | 4.0.0-rc-5 | 3.2.1 | `fa7e001da203d31fd5afb2921f90c3f428db2d5b` | 2144 | 3028/3285（92.18%） | [44356116107](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44356116107) |

上述记录覆盖文档事实和分支构建。最终集成门禁仍按第 3 节逐项验收。
