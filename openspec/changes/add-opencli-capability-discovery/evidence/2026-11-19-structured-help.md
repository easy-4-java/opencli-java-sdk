# OpenCLI 1.8.8 帮助协议采集

使用 Node 24.18.0 和 npm 发布包 `@jackwener/opencli@1.8.8`，运行包声明的入口 `dist/src/main.js`。上游 `v1.8.8` tag 指向 `8271afc67e8504bda94c147f446ee29775d08274`，与本项目 U02/U03 锁定一致。[上游帮助实现](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/help.ts)

[原始样本及 SHA-256](../../../../src/test/resources/opencli-contracts/v1/structured-help-1.8.8/) 按 stdout 字节保存。两轮分别使用独立临时用户目录，禁止自动更新检查，不加载用户已有的配置或插件。只运行 `--version` 和下表的帮助参数。两轮版本均为 1.8.8，9 个样本均退出 0、stderr 为空，对应 stdout 逐字节一致。样本来自 Apache-2.0 发布包。

| 样本 | 入口参数 | 实际输出 |
|---|---|---|
| `root.json` | `--help -f json` | 单一 JSON。根命令为简要列表，`site_adapters` 为含 `count`、`sites` 的对象。 |
| `browser.stdout` | `browser --help -f json` | JSON 后附加示例说明，共 472 个非首尾空白后缀字符，严格 JSON 读取失败。 |
| `browser-tab.json` | `browser fixture tab --help -f json` | `tab` 分组 JSON。命令路径保留 `<session>`，包括 `tab list`、`tab select`。 |
| `browser-get.json` | `browser fixture get --help -f json` | `get` 分组 JSON。包含 `get text`、`get attributes` 等叶子。该版本把 `tab` 和 `get` 分为两组。 |
| `browser-state.json` | `browser fixture state --help -f json` | 单个叶子，带 session 占位符和独立的三类 options。 |
| `meta-auth.json` | `auth --help -f json` | meta 命名空间 JSON，保留 namespace 与 global options。 |
| `meta-list.txt` | `list --help -f json` | 纯文本 Commander 帮助，严格 JSON 读取失败。 |
| `site-npm.json` | `npm --help -f json` | 站点目录 JSON，公共参数字段为 `common_options`。 |
| `site-npm-search.json` | `npm search --help -f json` | 叶子 JSON，含必填位置参数 `query` 和命令参数 `limit`。 |

采集时通过 `node --import` 预加载脚本，将进程的 `os.homedir()` 指向临时目录，并同步 Node 内置模块导出。为两轮分别配置 `OPENCLI_CACHE_DIR`、`OPENCLI_CONFIG_DIR`，设置 `CI=1`、`NO_COLOR=1`。浏览器参数 `fixture` 是帮助查询的字面会话名，未创建会话或执行叶子命令。

`OpenCliStructuredHelpFixtureTest` 验证真实的作用域、占位符、根命令目录与两种负例，报告位于 `target/surefire-reports`。这些输出约束后续 C06 模型设计。两种负例仍需发现服务给出明确状态。远端 Agent、运行时插件、本地 override 及其他站点尚无本轮采集证据。
