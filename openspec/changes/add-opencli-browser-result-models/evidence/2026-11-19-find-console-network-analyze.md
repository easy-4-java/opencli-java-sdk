# OpenCLI 1.8.8 的查找、采集与分析结果

使用 npm 发布包 `@jackwener/opencli@1.8.8`、Node 24.18.0 和 macOS arm64。调用 [锁定版 CLI action](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/cli.ts)。页面调用由隔离的 Browser Bridge 桩提供，命令层的 JSON 包装、采集归一化、分析计算和缓存逻辑使用发布包原实现。固定时间为 2025-01-01T00:00:00Z，测试数据来自本地桩。

| 叶子 | typed 入口 | 1.8.8 输出 | 验收范围 |
|---|---|---|---|
| tab list | `tabListTyped()` | 标签数组 | 普通、空列表 |
| open | `openTyped(url, tab)` | 导航对象 | 页 ID 与可选上下文 |
| state | `snapshotTyped(tab, source)` | URL 头和正文 | DOM、AX |
| state --compare-sources | `compareStateSourcesTyped(tab)` | 两侧指标对象 | 成功、AX 单侧失败 |
| find | `findTyped(locator, tab, options)` | 总匹配数和条目 | CSS、限量及精确 ref |
| console | `consoleTyped(tab, options)` | 单次采集对象 | `type`、`level` 和合法空数组 |
| network | `networkTyped(options, tab)` | 单次采集对象 | 请求条目、形状和扩展元数据 |
| analyze | `analyzeTyped(url, tab)` | 报告对象 | URL、标题、分类与扩展报告 |
| console/network --follow、network --detail、其余叶子 | raw 会话 API | 流或其它形状 | 保持 raw |

原始输出、SHA-256 与采集 preload 保存在 [样本目录](../../../../src/test/resources/opencli-contracts/v1/browser-output-1.8.8/)。沿用 [启动方式](2026-11-19-open-and-tab-list.md) 并设置 `OPENCLI_FIXTURE_FIXED_CLOCK=1`。调用 `browser fixture find --css '#fixture' --limit 1`、`browser fixture console`、`browser fixture network` 和 `browser fixture analyze http://127.0.0.1:18765/basic.html`。实际浏览器、网络请求和账户状态未参与本次验收。

typed 捕获返回原始执行结果。对现有异常中的 partial result，可按 leaf 显式调用 `OpenCliBrowserDecoder`。分析提示、网络详情提示与 sitemap 路径保留为数据，由调用者明确决定后续动作。流式和详情参数可继续交给现有 raw 方法。

`console-level.json` 由同一发布包运行 `browser fixture console --level warning` 采集。通过 `OPENCLI_FIXTURE_CONSOLE_LEVEL=1` 让隔离页面桩仅报告 `level`，CLI 仍返回该条记录。类型化结果优先读取 `type`，缺少时使用 `level`。原始元数据保留 CLI 输出字段。
