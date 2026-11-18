# OpenCLI 1.8.8 浏览器叶子输出

使用 npm 发布的 `@jackwener/opencli@1.8.8`、Node 24.18.0 和 macOS arm64。对版本锁定的 [CLI action](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/cli.ts) 使用真实命令入口和参数预处理。仅在 Browser Bridge 连接边界接入隔离页面桩，页面字段依据 [扩展输出](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/extension/src/background.ts) 提供。未连接浏览器账户、真实标签页或站点。

| 叶子 | 归档样本 | 验证内容 |
|---|---|---|
| `browser fixture tab list` | `tab-list.json`、`tab-list-empty.json` | 数组，保留前导零的 page 字符串及合法空列表 |
| `browser fixture open <url>` | `open.json` | 对象，包含实际 URL 和可选 page，允许缺少 sitemap |
| state、find、console、network、analyze | 待采集 | 原有 raw API |

在 [样本目录](../../../../src/test/resources/opencli-contracts/v1/browser-output-1.8.8/) 保存 stdout 原文、SHA-256 和可复跑的 preload。设置 `OPENCLI_ENTRY` 为锁定包的 `dist/src/main.js` 绝对路径后，可从该目录运行以下命令。

```bash
fixture_home=$(mktemp -d)
env HOME="$fixture_home" CI=1 NO_COLOR=1 node --import "$PWD/capture.mjs" "$OPENCLI_ENTRY" browser fixture tab list > "$fixture_home/tab-list.json"
env HOME="$fixture_home" CI=1 NO_COLOR=1 OPENCLI_FIXTURE_EMPTY=1 node --import "$PWD/capture.mjs" "$OPENCLI_ENTRY" browser fixture tab list > "$fixture_home/tab-list-empty.json"
env HOME="$fixture_home" CI=1 NO_COLOR=1 node --import "$PWD/capture.mjs" "$OPENCLI_ENTRY" browser fixture open http://127.0.0.1:18765/basic.html > "$fixture_home/open.json"
for specimen in tab-list.json tab-list-empty.json open.json; do
  cmp "$specimen" "$fixture_home/$specimen"
done
```

`session.tabListTyped()` 和 `session.openTyped(url, tab)` 返回带原始结果的严格视图。原有执行异常仍携带 partial result，可显式传给 `OpenCliBrowserDecoder` 查看截断或执行失败状态。扩展字段保留在 JSON 视图，路径和提示只作为数据。
