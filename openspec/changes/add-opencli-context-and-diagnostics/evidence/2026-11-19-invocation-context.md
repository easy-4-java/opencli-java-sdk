# OpenCLI 1.8.8 的单次调用上下文

使用 npm 发布包 `@jackwener/opencli@1.8.8`、Node 24.18.0 和隔离的 macOS arm64 用户目录。仅采集 `profile --help -f json` 和 `zhihu hot --help -f json`。原始 stdout 与 SHA-256 保存在 [结构化帮助样本目录](../../../../src/test/resources/opencli-contracts/v1/structured-help-1.8.8/)。

| 选项 | 证据 | SDK 绑定位置 |
|---|---|---|
| `--profile` | profile 与 Browser state 帮助中的 global_options | 根命令之前 |
| `--window` | Browser state namespace_options 或站点 browser_common_options | 浏览器会话名之后，站点参数位 |
| `--site-session`、`--keep-tab` | 已声明 browser=true 的 zhihu hot browser_common_options | 站点参数位 |
| `--trace` | 站点 common_options | 站点参数位 |

[argv 预处理](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/cli-argv-preprocess.ts) 要求 Browser 之后先读到会话名。[站点适配器注册器](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/commanderAdapter.ts) 从根选项读取 profile 并单独读取 Browser 通用选项。SDK 在已验证的 `zhihu hot` 范围需要 profile 时，只取当前 profile 帮助的根参数定义。旧的 Browser 会话入口同步调整 `--window` 位置。

```java
OpenCliInvocationContext defaults = OpenCliInvocationContext.builder().keepTab(true).build();
properties.setInvocationDefaults(defaults);
OpenCliInvocationContext request = OpenCliInvocationContext.builder().keepTab(false).build();
client.invokeDiscovered(OpenCliHelpScope.SITE_ZHIHU_HOT, "opencli zhihu hot",
    null, null, null, request);
```

显式上下文和调用选项覆盖默认值。两种显式输入冲突时拒绝提交。默认值只应用于发现后严格校验的本地调用。业务调用按调用者选中的叶子正常执行。SDK 不修改环境或全局 profile。只读诊断留待 C08 后续验收。
