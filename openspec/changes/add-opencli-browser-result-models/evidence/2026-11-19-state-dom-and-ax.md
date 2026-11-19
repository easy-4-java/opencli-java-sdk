# OpenCLI 1.8.8 的 DOM 和 AX 状态输出

使用 npm 发布包 `@jackwener/opencli@1.8.8`、Node 24.18.0 和 macOS arm64。调用 [锁定版 state 命令入口](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/cli.ts)。在 Browser Bridge 页面边界返回受控 DOM 文本，并用 [上游 AX 渲染器](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/browser/ax-snapshot.ts) 渲染合成 CDP 树。对比样本用固定时钟消除 elapsed_ms 波动，并另采集 AX 单侧失败场景。浏览器扩展和实际 CDP 链路仍待验证。

| 叶子调用 | stdout 样本 | 结果 |
|---|---|---|
| `state --source dom` | `state-dom.stdout` | URL 头、空行和 DOM 正文 |
| `state --source ax` | `state-ax.stdout` | URL 头、空行和 AX 正文 |
| `state --source ax --compare-sources` | `state-compare.json` | JSON，DOM 和 AX 各自报告指标 |
| `state --compare-sources`，注入 AX 不可用 | `state-compare-ax-failure.json` | 进程成功，DOM 指标和 AX 错误并存 |

[样本目录](../../../../src/test/resources/opencli-contracts/v1/browser-output-1.8.8/) 保存原始 stdout 和 SHA-256。沿用 [前一批样本](2026-11-19-open-and-tab-list.md) 的启动方式，将命令替换为上表的 state 调用并设置 `OPENCLI_FIXTURE_FIXED_CLOCK=1`。最后一项额外设置 `OPENCLI_FIXTURE_AX_FAILURE=1`。

`session.snapshotTyped(tab, OpenCliBrowserSnapshotSource.AX)` 返回 URL 和原样的快照正文。DOM 和 AX 对比使用 `session.compareStateSourcesTyped(tab)`，每侧分别检查 `isOk()`，失败侧可读取错误信息。调用者仍可通过原有 `state(tab, options)` 传递未来的字符串 source 或组合已有参数。
