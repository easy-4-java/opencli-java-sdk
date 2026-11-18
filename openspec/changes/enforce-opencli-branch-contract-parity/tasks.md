# C10 Tasks · 共享契约测试、分支差异与上游锁定

> 已验收的准备任务见下方证据。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-branch-contract-parity/spec.md)。

## 1. 基础阶段可先行

- [ ] 1.1 记录三个精确HEAD和允许差异矩阵，确定first-implementation分支经评审认可。
- [x] 1.2 建立共享contractfixture目录、来源lock与测试runner，复用现有coverage而不删除其它测试。
- [ ] 1.3 定义完整argv/协议/typed/live分层报告，加入0测试和missingreport失败规则。

## 2. 上游与文档

- [x] 2.1 采集锁定上游的实际help/manifest/output，保存hash与范围，移除对作者绝对路径的依赖。
- [x] 2.2 修正各分支README/POMdescription/Gradle示例，检查Java/Jackson/Maven实际声明。
- [x] 2.3 配置三线正确wrapper/JDK的独立构建与报告，不通过修改coverage阈值绕开行为问题。

## 3. 最终集成门禁

- [ ] 3.1 在C01–C09选定实现完成后，分别在三线执行全部对应正负fixtures，记录HEAD与真实退出结果。
- [x] 3.2 运行官方OpenSpec逐Change与all strict校验，保存CLI版本和报告；环境失败保持NOT_RUN。
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

### 结构化帮助证据门禁

OC-PARITY-003/004：在 `98f28346e7bc3307608122a0086a8e1072e39536` 的 GitHub 工件中，Surefire 记录了三个通过的 `OpenCliStructuredHelpFixtureTest` 用例，契约报告的枚举层仍显示 `NOT_RUN`。从该工件重放报告命令并声明枚举套件后，枚举层显示 `PASS`、执行三项，真实 CLI 执行层仍为 `NOT_RUN`。移除套件报告时，报告命令返回非零。三条开发线使用同一份 SHA-256 清单检查九个帮助样本，在临时副本的 `root.json` 末尾追加换行会使校验失败。样本提供离线结构证据，实时能力发现和真实执行由对应任务另行验收。

### 每个必选套件的执行证据

OC-PARITY-003/004：当同一层含一个已执行套件及一个零用例套件时，旧报告返回 PASS。把同一套件注册到 argv 与 real-execution 也会重复记账。新增两个失败回归后，报告逐套件检查非零执行、零跳过、零失败及零错误，并拒绝同一套件跨层归因。问题套件列在 `unprovenSuites`。

JVM 和平台报告显式登记错误报文、浏览器 typed 结果、上下文和诊断测试。Windows npm 安装版检查用独立 `installed-report.json`，保留实际 Node、OpenCLI、Java 和 Maven 版本、源码 SHA 与执行退出码。真实 CLI 层的范围为安装版 launcher 与隔离的 help、插件和诊断测试。普通 JVM 或平台报告缺少此层时保持 NOT_RUN。用于 Browser 结果解析的全部归档样本继续按 SHA-256 验证。

### OC-PARITY-003 运行间证据隔离

在 `6da93d05` 用两个旧的通过报告和一个成功退出但未执行测试的 Maven 替身复现错误通过。安装契约脚本现在在启动 Maven 前将已有 Surefire 报告归档到 `platform-evidence/pre-install-surefire.*`，本次门禁只读取重新生成的 `target/surefire-reports`。回归中真实执行层从错误的 `PASS` 改为 `FAIL`，执行数为 0，退出码为 1，旧报告仍可回查。

在 `e858cc0`、`6da93d05`、`c22fe45` 加入修复后，三线的 `python3 -m unittest discover -s scripts/tests` 各通过 16 项。使用本地缓存的已安装 OpenCLI 1.8.8 依赖运行同一脚本，Java 8、17、21 分别用本线 Wrapper 实际执行覆盖和只读诊断集成测试，各通过 2 项，零跳过。报告为 `platform-evidence/installed-report.json`。

### OC-PARITY-002/004 共享来源锁定与三线报告核对

基线 Java 8 `876a48a`、Java 17 `004c3286`、Java 21 `a384a5b` 的托管 JVM 报告分别记录 2231、2232、2231 项执行，零失败、零错误、零跳过。对应 Windows 工件的真实安装层各执行 4 项，零跳过。Windows 进程层分别记录 40、38、40 项。Java 17 文件树包含 `SubprocessStreamFailureTest`，但该套件未进入它的平台执行和报告配置。补齐后，JVM 和平台报告都要求该套件执行。

Java 21 基线的共享校验清单引用了未被 Git 跟踪的 `sources.lock.json`，`shasum -a 256 -c SHA256SUMS` 返回非零。从 Java 17 基线恢复相同的来源锁定文件后，三条线均校验 28 个文件。临时副本中追加一个换行会使校验退出 1。三条常规 CI 在 Maven 构建前校验共享来源锁定、结构化帮助及浏览器输出三份清单。

使用 OpenSpec 1.14.1 在三条基线逐项运行 `openspec validate <change> --strict --no-interactive` 并执行 `openspec validate --all --strict --no-interactive`，每条线均通过 10 个变更，退出 0。Java 17 基线的 [托管工件](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44382023710) 另记录固定版本 1.13.1 的逐项结果和 `all.log`，同样通过。此处记录规格校验与工件事实。独立评审与归档仍按最终门禁验收。

### 2026-11-19 上游 manifest 采集

OC-PARITY-002：从 npm 发布包提取 `package/cli-manifest.json`。源为 https://registry.npmjs.org/@jackwener/opencli/-/opencli-1.8.8.tgz，包的 SHA-512 完整性与 npm registry 元数据一致。原始 manifest 的 SHA-256 为 `b6f44ab5973dfd46b001f42562ad520b184bf1dd27ec4ae807f849df70b98e6a`。范围为发布包自带的 adapter 命令枚举，共 1,366 条、179 个 site。按 `(site, name)` 逐项对比仓库历史快照，新增 89 条，移除 0 条。89 条新增命令的逐项差异保存在本节链接的来源锁定中。

在临时目录采集并运行生成器后，报告只记录公开来源、原始字节的 SHA-256、观察日期、获取命令和枚举范围。历史报告仍用于原有 Java 测试。生成器通过 `--manifest` 或 `OPENCLI_MANIFEST` 读取本地输入，移植验证覆盖含空格的临时路径。

```bash
curl -fsSLo opencli-1.8.8.tgz https://registry.npmjs.org/@jackwener/opencli/-/opencli-1.8.8.tgz
tar -xOf opencli-1.8.8.tgz package/cli-manifest.json > cli-manifest.json
```


### OpenCLI 1.8.8 全量样本复跑

[上游来源锁定](../../../src/test/resources/opencli-contracts/v1/upstream-1.8.8.lock.json) 记录 manifest 采集命令及逐项差异，以及帮助和浏览器输出的校验清单与复跑入口。使用 Node 24.18.0 和已安装的 1.8.8 包，在独立临时配置、缓存及用户目录中执行。11 份帮助、12 份浏览器叶子输出均与已归档的 stdout 逐字节相同，全部退出 0，stderr 为空。帮助包含非 JSON 输出。浏览器叶子使用真实 CLI 与隔离页面桩，真实浏览器联调证据单独见 [Browser Bridge 验收](../add-opencli-browser-result-models/evidence/2026-11-19-real-browser-bridge.md)。历史覆盖基线的升级仍须评审。
