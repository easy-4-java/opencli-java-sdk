# OpenCLI Java SDK 复核文档与 OpenSpec 提案包

日期：2026-09-21。交付类型：**文档 / 规范草案，不含 Java 实现**。

## 阅读入口

1. [三分支复核报告](docs/architecture/opencli-java-sdk-reverification.md)：已确认问题、上一轮纠正、证据边界。
2. [目标架构](docs/architecture/opencli-java-sdk-target-architecture.md)：现有调用链、演进边界、资源/错误/远程模型。
3. [能力矩阵](docs/architecture/opencli-java-sdk-capability-matrix.md)：已有、待优化、暂缓项。
4. [OpenSpec 路线图](openspec/README.md)：十个 Change、依赖、评审与实施顺序。
5. [证据索引](docs/architecture/opencli-java-sdk-evidence.md) 与 [锁定信息](docs/architecture/opencli-java-sdk-sources.lock.json)。
6. [文档验证报告](docs/architecture/opencli-java-sdk-validation.md)：实际执行的格式检查与未执行项。

## 保存与状态

本包最初在对话工作容器生成；原始生成阶段未提交/推送。当前提交版在用户授权后，通过 GitHub 连接器追加到三个 feature 分支，不改动既有 Java/POM/CI，不改 main，也不表示用户 Mac 工作区已同步。提交前发现的分支前移、coverage 补回和快照计数变化，见 [提交前复核补记](docs/architecture/opencli-java-sdk-publish-check.md)。具体提交与推送结果以远端分支和提交回读为准。

所有 Change 的状态为 DRAFT / REVIEW_REQUIRED。`openspec/specs/` 暂不放“已生效规格”；待变更评审、实现、验收后再由 OpenSpec archive 流程合入。当前使用 `ADDED Requirements` 表示首次将行为写入正式规格，不代表所有底层能力都是首次实现。

## 安全应用到仓库

完整 ZIP 保持仓库相对目录结构；配套 patch 是仅新增文档/规范的补丁。先在目标 checkout 确认分支、已有改动和同名文件，再检查补丁。以下命令是用户本地待执行步骤，不是本轮执行记录：

```bash
# 在 opencli-java-sdk 目标 checkout 内执行；PATCH 指向本次下载的 .patch。
git status --short --branch
git apply --stat "$PATCH"
git apply --check "$PATCH"
# 上一步通过并确认路径内容后再应用。
git apply "$PATCH"
git diff --check
```

如果目标已有 `openspec/config.yaml` 或同名文档导致冲突，不要覆盖或强制应用；将现有规则与本包上下文人工合并。新增未跟踪文件不会自动出现在普通 `git diff` 中，应结合 `git status` 检查；提交、推送、创建 PR 不由本包自动执行。

## 官方 OpenSpec 复验

在具备可用 OpenSpec CLI 的环境、目标仓库根目录执行：

```bash
openspec --version
openspec list --json
openspec validate --all --strict --no-interactive
# 或逐个验证；以实际安装版本的 --help 为准。
openspec validate harden-opencli-argv-contract --strict --no-interactive
```

保留工具版本、命令、退出码与完整报告。本轮安装失败，**官方 strict 校验未执行**；本包通过的自定义结构检查不能替代它，也不能替代需求评审或代码测试。

## CodeGraph 后续证据

在三个独立 checkout/worktree 中固定本包记录的 HEAD，确认 CodeGraph 安装后分别建立索引，查询 Executor、SubprocessExecutionSupport、ArgvToCollectParser、ReverseAgentClient 的调用者/被调用者和影响范围。记录工具版本、ref、索引结果和查询输出。无需先注册 MCP 才能使用 CLI。本轮没有产生 CodeGraph 结果，不能声称该步骤完成。

## 实现前门禁

先评审规范，确认兼容迁移和默认资源预算；再按 Change 任务做 TDD。不得因为任务文件已经生成就勾选实现完成。不得通过批量执行真实站点 publish/delete/send 来刷覆盖数量。

## 交付校验

`SHA256SUMS`包含其余全部文件的SHA-256。配套ZIP和patch的大小、hash与临时目录应用校验见独立交付收据。包内只有提案文档、规格、合成测试向量和文档检查器，没有产品实现。
