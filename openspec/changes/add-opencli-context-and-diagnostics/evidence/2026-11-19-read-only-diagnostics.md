# OpenCLI 1.8.8 只读诊断

已核对 npm 包 `@jackwener/opencli@1.8.8` 的 [启动入口](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/main.ts)。顶层 `--version` 在插件加载、后台更新检查和 `onStartup` 前退出。完整启动路径会创建用户适配器目录并执行启动钩子，因此即便只查询帮助，诊断也不进入完整启动路径。调用者配置的绝对 launcher、前置参数及运行环境属于信任边界。此处的只读证据对应锁定版实际 CLI。

| 检查 | 来源及适用性 |
|---|---|
| EXECUTABLE | 本机绝对路径及可执行权限检查 |
| VERSION | 有界版本 fast path |
| STRUCTURED_HELP | 历史显式发现，配置与版本匹配才引用。已成功样本标为 STALE，失败样本标为 UNKNOWN |
| BROWSER_BRIDGE、AUTHENTICATION、DAEMON | UNKNOWN，未确认只读探测 |
| REMOTE_TRANSPORT | legacy HTTP 模式为 UNKNOWN，诊断不发送 collect |
| 远端模式的本机检查 | NOT_APPLICABLE |

```java
OpenCliDiagnostics diagnostics = client.diagnose();
OpenCliDiagnostics.Observation version =
    diagnostics.observation(OpenCliDiagnostics.Check.VERSION);
OpenCliDiagnostics.Observation help =
    diagnostics.observation(OpenCliDiagnostics.Check.STRUCTURED_HELP);
```

`OpenCliDiagnosticsTest` 记录子进程的每条命令，验证仅版本探测、历史帮助失败与成功、配置改变以及远端跳过。`OpenCliReadOnlyDiagnosticsIT` 在临时用户目录注册一个只写入测试标记的启动钩子。诊断取得真实版本而未写标记。显式帮助启动会写入标记。移除标记后再次诊断，历史帮助仍可追溯，标记仍未出现。没有真实登录、认证续期、daemon 重启或外部请求。

`open` 与 `analyze` 的可选 sitemap 与 trace 按各自结果元数据保留，缺失仍是缺失。没有 overlay 文件访问。Pipeline 保持 [能力矩阵](../../../../docs/architecture/opencli-java-sdk-capability-matrix.md) 中的 JS 模块／DEFERRED 状态，Java 批处理模型由独立 ADR 评审。
