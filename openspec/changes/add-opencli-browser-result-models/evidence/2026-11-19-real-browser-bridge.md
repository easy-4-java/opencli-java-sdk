# 真实 Browser Bridge 验收

以 `afeb522`、`3c9979ec`、`5c35d33` 为基线，使用 macOS arm64、Chrome for Testing 158.0.8127.18、Node 24.18.0、OpenCLI 1.8.8 与 [Browser Bridge 1.0.24 发布包](https://github.com/jackwener/OpenCLI/releases/tag/v1.8.8)。扩展 ZIP 的 SHA-256 为 `bad9163f32a66224404e302f52f35391672341d2bd0bf30c8bbaae3c6e6e246c`，与发布记录一致。

在临时浏览器 profile 加载扩展。为避开宿主机已有 daemon，仅在两份测试副本改用独立回环端口。OpenCLI 包的 `dist/src/constants.js` 与扩展的 `dist/background.js` 使用同一测试端口，扩展的 daemon host 显式设为 `127.0.0.1`。实际扩展通过 WebSocket 连上真实 daemon。浏览器使用无头模式和临时钥匙串，页面来自独立的本地 HTTP 服务。

| 入口 | 验收 |
| --- | --- |
| `openTyped` 与 `tabListTyped` | 导航到测试 URL，列表含相同的活动 page ID |
| `snapshotTyped(DOM)` | 相同 URL，含测试页面按钮文本 |
| `snapshotTyped(AX)` | 相同 URL，含测试页面的可访问性根节点和标题 |
| `compareStateSourcesTyped` | DOM 和 AX 均报告成功 |

三条 JDK 线分别执行 `./mvnw -B -ntp -Dtest=OpenCliBrowserBridgeIT test`，各通过 1 项，零失败、零错误、零跳过。报告位于 `target/surefire-reports/TEST-io.github.easy4j.opencli.contract.OpenCliBrowserBridgeIT.xml`。

复跑时提供独立运行的测试 daemon、加载该扩展的临时浏览器，以及 `http://127.0.0.1:<port>/fixture.html`，页面标题为 `Bridge fixture` 且含 `Continue` 按钮。设置 `OPENCLI_INSTALLED_NODE` 为 Node 可执行文件，`OPENCLI_INSTALLED_ENTRY` 为测试 CLI 的 `dist/src/main.js`，`OPENCLI_BRIDGE_FIXTURE_URL` 为本地页面 URL，然后执行上述 Maven 命令。

本次 AX 返回页面根节点，未观察到按钮节点。真实账户、外部站点、其余 leaf 及 macOS GitHub runner 不属于本次证据范围。
