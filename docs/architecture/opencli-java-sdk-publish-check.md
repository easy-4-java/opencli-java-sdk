# GitHub 提交前复核补记

日期：2026-09-21。范围：提交上一轮架构文档与 OpenSpec 提案，不实施产品功能。

## 1. 当前提交基线与历史审计基线分离

通过 GitHub 连接器重新读取分支与 Git commit/tree。以下为本轮提交的已观察 parent；如提交期间分支再次前移，必须重新读取并基于新 parent 追加，不允许 force push。

| 分支 | 已观察 HEAD | tree |
|---|---|---|
| feature/1.0.x | `c9a6ced5823126c092839aaf35386ceb5ae936e4` | `0a4ba08aa8bc6e3d535180c73366c9a07c946dbd` |
| feature/2.0.x | `ce8ece5447b9747d67274bf31fc90baadd2681f1` | `44ac52070a715561106c73379132635cdb8111e7` |
| feature/3.0.x | `14013f0235892fa0848bd0d11748f8a2088b2d2d` | `5ca4ff198d24794fa06de91e864be5b53f57b353` |

原始 [复核报告](opencli-java-sdk-reverification.md)、[证据索引](opencli-java-sdk-evidence.md) 和 [sources.lock.json](opencli-java-sdk-sources.lock.json) 固定的是此前快照；保留其来源，不伪装为已在新 HEAD 重新运行完整审计。

## 2. 已发生的上游仓库变化

2.x / 3.x 的新提交已正向同步五个 coverage 测试及两个资源；3.x 含 Jackson 3 适配。两条提交也记录了环境依赖测试修正与作者注释规范化。1.x 最新提交修正同一环境依赖测试。因此不再把“恢复缺失的 coverage 目录”作为当前未完成工作。

当前 3.x 的 `manifest-coverage-meta.json` 实际内容为：`manifestCommandCount: 1277`、`siteCount: 173`，来源为 `https://github.com/jackwener/opencli cli-manifest.json @ main (f802942c)`，而非原先作者本机绝对路径。1275/173只保留为历史审计快照；1277同样只是该测试资源的计数，不是全部真实站点运行通过。

C10继续负责完整参数断言、来源版本/完整ref/hash/采集时刻、分层覆盖报告、三分支执行证据；不重复已落地工作，不因目录存在就勾选验收。

固定来源：

- [1.x提交](https://github.com/easy-4-java/opencli-java-sdk/commit/c9a6ced5823126c092839aaf35386ceb5ae936e4)
- [2.x提交](https://github.com/easy-4-java/opencli-java-sdk/commit/ce8ece5447b9747d67274bf31fc90baadd2681f1)
- [3.x提交](https://github.com/easy-4-java/opencli-java-sdk/commit/14013f0235892fa0848bd0d11748f8a2088b2d2d)
- [3.x当前测试资源](https://github.com/easy-4-java/opencli-java-sdk/blob/14013f0235892fa0848bd0d11748f8a2088b2d2d/src/test/resources/opencli/manifest-coverage-meta.json)

## 3. 提交范围与保护边界

原始ZIP含65个文件，CRC与原始SHA256SUMS已复核。本提交版添加本补记，并更新阅读入口、历史说明、C10上下文与文档检查报告；不修改59条Requirement和118个Scenario的验收意图，84项实现任务全部未勾选。

三个已观察根tree均不存在 `docs/`、`openspec/`、`README-OPENCLI-SPEC-PACK.md` 或 `SHA256SUMS`，因此采用仅新增文档的tree，保留所有既有条目的blob/tree SHA。三个分支共用同一份文档内容，分别以各自最新commit为parent。main不在本次写入范围。

普通Git网络尝试返回 `Could not resolve host: github.com`；发布通过已连接GitHub的Git data API完成，不伪称已在Mac执行git push或同步其工作区。最终成功必须以create commit、非强制update ref和回读HEAD为证据；本文件本身不是推送成功证明。

## 4. 验证与限制

提交前重新执行包内自定义结构检查及七个负例，并检查新增文件差异和完整性。自定义检查不等于官方OpenSpec strict，也不等于SDK单测。

官方OpenSpec strict、CodeGraph索引、Maven三分支测试及真实OpenCLI smoke在本次未执行。GitHub CI以新提交关联的实际运行结果为准；未触发或pending不记为pass。规范保持DRAFT / REVIEW_REQUIRED，产品实现仍为NOT_STARTED。
