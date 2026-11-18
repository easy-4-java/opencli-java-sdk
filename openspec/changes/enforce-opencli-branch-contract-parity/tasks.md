# C10 Tasks · 共享契约测试、分支差异与上游锁定

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-branch-contract-parity/spec.md)。

## 1. 基础阶段可先行

- [ ] 1.1 记录三个精确HEAD和允许差异矩阵，确定first-implementation分支经评审认可。
- [ ] 1.2 建立共享contractfixture目录、来源lock与测试runner，复用现有coverage而不删除其它测试。
- [ ] 1.3 定义完整argv/协议/typed/live分层报告，加入0测试和missingreport失败规则。

## 2. 上游与文档

- [ ] 2.1 采集锁定上游的实际help/manifest/output，保存hash与范围，移除对作者绝对路径的依赖。
- [ ] 2.2 修正各分支README/POMdescription/Gradle示例，检查Java/Jackson/Maven实际声明。
- [ ] 2.3 配置三线正确wrapper/JDK的独立构建与报告，不通过修改coverage阈值绕开行为问题。

## 3. 最终集成门禁

- [ ] 3.1 在C01–C09选定实现完成后，分别在三线执行全部对应正负fixtures，记录HEAD与真实退出结果。
- [ ] 3.2 运行官方OpenSpec逐Change与all strict校验，保存CLI版本和报告；环境失败保持NOT_RUN。
- [ ] 3.3 核对Requirement到测试/commit的闭环并进行独立代码评审，才允许勾选和archive。
- [ ] 3.4 提交/推送/PR/发布仅在用户明确授权时执行；观察实际CI，不承诺未发生的远端结果。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

### 每个必选套件的执行证据

OC-PARITY-003/004：当同一层含一个已执行套件及一个零用例套件时，旧报告返回 PASS。把同一套件注册到 argv 与 real-execution 也会重复记账。新增两个失败回归后，报告逐套件检查非零执行、零跳过、零失败及零错误，并拒绝同一套件跨层归因。问题套件列在 `unprovenSuites`。

JVM 和平台报告显式登记错误报文、浏览器 typed 结果、上下文和诊断测试。Windows npm 安装版检查用独立 `installed-report.json`，保留实际 Node、OpenCLI、Java 和 Maven 版本、源码 SHA 与执行退出码。真实 CLI 层的范围为安装版 launcher 与隔离的 help、插件和诊断测试。普通 JVM 或平台报告缺少此层时保持 NOT_RUN。用于 Browser 结果解析的全部归档样本继续按 SHA-256 验证。

### 2026-11-19 上游 manifest 采集

OC-PARITY-002：从 npm 发布包提取 `package/cli-manifest.json`。源为 https://registry.npmjs.org/@jackwener/opencli/-/opencli-1.8.8.tgz，包的 SHA-512 完整性与 npm registry 元数据一致。原始 manifest 的 SHA-256 为 `b6f44ab5973dfd46b001f42562ad520b184bf1dd27ec4ae807f849df70b98e6a`。范围为发布包自带的 adapter 命令枚举，共 1,366 条、179 个 site。按 `(site, name)` 逐项对比仓库历史快照，新增 89 条，移除 0 条。任务 2.1 仍待汇总 help 和输出样本并评审快照变更。

在临时目录采集并运行生成器后，报告只记录公开来源、原始字节的 SHA-256、观察日期、获取命令和枚举范围。历史报告仍用于原有 Java 测试。生成器通过 `--manifest` 或 `OPENCLI_MANIFEST` 读取本地输入，移植验证覆盖含空格的临时路径。

```bash
curl -fsSLo opencli-1.8.8.tgz https://registry.npmjs.org/@jackwener/opencli/-/opencli-1.8.8.tgz
tar -xOf opencli-1.8.8.tgz package/cli-manifest.json > cli-manifest.json
```
