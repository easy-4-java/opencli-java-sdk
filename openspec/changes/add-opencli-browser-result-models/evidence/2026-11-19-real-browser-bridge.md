# 真实 Browser Bridge 验收

使用 macOS arm64、Chrome for Testing 158.0.8127.18、Node 24.18.0、OpenCLI 1.8.8 与 [Browser Bridge 1.0.24 发布包](https://github.com/jackwener/OpenCLI/releases/tag/v1.8.8)。扩展 ZIP 的 SHA-256 为 `bad9163f32a66224404e302f52f35391672341d2bd0bf30c8bbaae3c6e6e246c`，与发布记录一致。

在临时浏览器 profile 加载扩展。为避开宿主机已有 daemon，仅在两份测试副本改用独立回环端口。OpenCLI 包的 `dist/src/constants.js` 与扩展的 `dist/background.js` 使用同一测试端口，扩展的 daemon host 显式设为 `127.0.0.1`。实际扩展通过 WebSocket 连上真实 daemon。浏览器使用无头模式和临时钥匙串，页面来自独立的本地 HTTP 服务。

| 入口 | 验收 |
| --- | --- |
| `openTyped` 与 `tabListTyped` | 导航到测试 URL，列表含相同的 page ID，活动状态与原始字段一致 |
| `snapshotTyped(DOM)` | 相同 URL，含测试页面按钮文本 |
| `snapshotTyped(AX)` | 相同 URL，含测试页面的可访问性根节点和标题 |
| `compareStateSourcesTyped` | DOM 和 AX 均报告成功 |
| `findTyped` | CSS `#go` 返回唯一按钮、原始文本和 ref |
| `consoleTyped` | 返回该 session 的有效空集合 |
| `networkTyped` | 捕获本地 `GET /api/items` 和 HTTP 200 |
| `analyzeTyped` | 报告真实页面 URL 与标题 |

以 `e7ca1c0`、`a35219a5`、`15126bf` 为基线，三条 JDK 线分别执行 `./mvnw -B -ntp -Dtest=OpenCliBrowserBridgeIT test`，各通过 2 项，零失败、零错误、零跳过。报告位于 `target/surefire-reports/TEST-io.github.easy4j.opencli.contract.OpenCliBrowserBridgeIT.xml`。

复跑时提供独立运行的测试 daemon、加载该扩展的临时浏览器，以及 `http://127.0.0.1:<port>/fixture.html`，页面使用下方 HTML，服务器为 `/api/items` 返回 `application/json` 和 `{"items":[{"id":17}]}`。设置 `OPENCLI_INSTALLED_NODE` 为 Node 可执行文件，`OPENCLI_INSTALLED_ENTRY` 为测试 CLI 的 `dist/src/main.js`，`OPENCLI_BRIDGE_FIXTURE_URL` 为本地页面 URL，然后执行上述 Maven 命令。

```html
<!doctype html>
<title>Bridge fixture</title>
<main><button id="go">Continue</button></main>
<script>
const capture = setInterval(() => {
  console.warn('Bridge fixture warning');
  fetch('/api/items');
}, 250);
setTimeout(() => clearInterval(capture), 45000);
</script>
```

本次无头 AX 返回页面根节点，未观察到按钮节点。Chrome 日志收到了测试 warning，OpenCLI 1.8.8 的真实扩展路径仍返回空控制台列表。非空控制台记录由隔离页面桩测试覆盖。真实账户、外部站点及托管 macOS runner 未参与。
