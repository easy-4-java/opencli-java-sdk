# OpenCLI Java SDK 复核证据索引

> 状态：本轮源码读取证据。所有 SDK / OpenCLI 链接固定到 commit；不等于已运行这些程序。

## 分支与目录观察

- GitHub branches API 返回三个 feature HEAD 与上轮一致，见 sources.lock.json。
- 1.x coverage 目录读取成功，列出五个 CoverageTest；2.x、3.x 同目录 API 各返回 404。结合其它同 ref 文件可读，记录为该路径不存在；不推断整个分支没有其它测试，也不推断历史删除动机。
- 本轮根目录读取存在截断，未据此断言已有 docs/openspec 全部为空；本包作为新增路径交付，实际应用前必须运行 git apply --check，冲突时合并而非覆盖。

## 文件证据

### E01 — `src/main/java/io/github/easy4j/opencli/core/OpenCliExecutor.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/core/OpenCliExecutor.java)；读取范围：全文。
核对符号：`invoke / appendCleanArgs / run / completeAfterWait`。
本地 argv 经 trim 且空白 token 被丢弃；运行时分支选择本地或 legacy collect；失败日志及异常文本包含 commandLine。
Git blob：`ac1d8466dc9e7dc15e36cd2bdeb79fc2f8020661`。

### E02 — `src/main/java/io/github/easy4j/opencli/core/OpenCliAdapterChannel.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/core/OpenCliAdapterChannel.java)；读取范围：全文。
核对符号：`invoke / summarizeSubcommand`。
通用 Adapter 通道及结构化请求入口已存在；通道再次 trim 参数，debug 摘要会拼接前几个参数。
Git blob：`cfb19c473d86205f35c296fc1d7260296db248c6`。

### E03 — `src/main/java/io/github/easy4j/opencli/browser/support/OpenCliBrowserStateOptions.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/browser/support/OpenCliBrowserStateOptions.java)；读取范围：全文。
核对符号：`appendTo`。
已有 source 与 compareSources，分别输出 --source 和 --compare-sources。
Git blob：`f271b73c5943e00047f8e8bf1690b5f30e7f362e`。

### E04 — `src/main/java/io/github/easy4j/opencli/core/support/SubprocessExecutionSupport.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/core/support/SubprocessExecutionSupport.java)；读取范围：全文。
核对符号：`configureMaxConcurrentExecutions / execute / executeWithinLimit / awaitResult`。
进程并发是静态可替换 Semaphore；acquire 无超时；两个 ByteArrayOutputStream 无上限；中断路径无显式进程清理 finally。
Git blob：`bab1bc7684d5ffcc159f8784e9cdf50687ccbfec`。

### E05 — `src/main/java/io/github/easy4j/opencli/remote/OpenCliArgvToCollectParser.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/remote/OpenCliArgvToCollectParser.java)；读取范围：全文。
核对符号：`parse / normalizeKey`。
前两个 token 固定解析 site、command；使用 Map 保存选项；trim 参数并小写选项名；不识别 -- 终止符；负数下一参数可能被当作选项。
Git blob：`c4523656b65397d6545e6133ff16bff72c52865a`。

### E06 — `src/main/java/io/github/easy4j/opencli/remote/OpenCliRemoteAgentHttpClient.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/remote/OpenCliRemoteAgentHttpClient.java)；读取范围：全文。
核对符号：`collect / mapResponse / AgentCollectEnvelope`。
对接 opencli-admin POST /collect；只读取 success/items/error；映射为本地 0/1；非 2xx 响应前 500 字符和远端 error 会写日志。
Git blob：`3949507a5526c7c9dee8538d3a2a14ae72166e38`。

### E07 — `src/main/java/io/github/easy4j/opencli/center/ws/OpenCliWsReverseAgentClient.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/center/ws/OpenCliWsReverseAgentClient.java)；读取范围：全文。
核对符号：`collectPool / handleTextMessage / runCollectSafe / stop / close`。
反向边缘 Worker，不是应用端 WS RPC transport；cachedThreadPool 无界；collect 不检查 registered 完成；未见 request_id 去重；close 不等待在途任务。
Git blob：`3320285bcc7a46330b5ae896035c80c8025257b1`。

### E08 — `src/main/java/io/github/easy4j/opencli/core/OpenCliOutputParser.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/core/OpenCliOutputParser.java)；读取范围：全文。
核对符号：`parseBestEffort`。
只判断首尾括号的 JSON shape；不是错误协议解析。
Git blob：`97820a9382de0600580ec916017e47d7594dd0d4`。

### E09 — `src/main/java/io/github/easy4j/opencli/parser/OpenCliStdoutJson.java`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/main/java/io/github/easy4j/opencli/parser/OpenCliStdoutJson.java)；读取范围：全文。
核对符号：`parseLenient / typed`。
已有 JsonNode 泛型包装；解析失败返回 text node；尚不能当作 Browser 领域 DTO。
Git blob：`abfcdccde33b2551150b06aacdcbce9b22a5b236`。

### E10 — `src/test/resources/opencli/manifest-coverage-meta.json`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/src/test/resources/opencli/manifest-coverage-meta.json)；读取范围：全文。
核对符号：`manifestCommandCount / siteCount`。
记录的是 1275 条命令、173 个站点和本机绝对路径；不含上游 commit/version/hash。
Git blob：`09d64ff51911fc006955a55cbcab724129dbbe9b`。

### E11 — `src/test/java/io/github/easy4j/opencli/coverage/OpenCliAdapterCommandsCoverageTest.java`
来源：[easy-4-java/opencli-java-sdk @ 2095f617d567](https://github.com/easy-4-java/opencli-java-sdk/blob/2095f617d56789569d32cffe2cdedffe595270ba/src/test/java/io/github/easy4j/opencli/coverage/OpenCliAdapterCommandsCoverageTest.java)；读取范围：全文。
核对符号：`adapterCommand`。
使用 RecordingOpenCliExecutor；只检查非空结果与 argv 前两项，不执行真实 OpenCLI。
Git blob：`87236529ae5cde55061f25eb64a156f526a7620f`。

### E12 — `README.md`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/README.md)；读取范围：1–180。
核对符号：`项目说明 / 兼容矩阵 / 使用示例`。
明确已有通用 Adapter、Meta、Browser、HTTP 和 WS；3.x 文档同时存在 JDK 8、Jackson 2.17.x、错误 Gradle 版本示例等陈旧内容。
Git blob：`042a347e9f6ad9a2e69d9bf8e8c13a1e591b0724`。

### E13 — `pom.xml`
来源：[easy-4-java/opencli-java-sdk @ 2095f617d567](https://github.com/easy-4-java/opencli-java-sdk/blob/2095f617d56789569d32cffe2cdedffe595270ba/pom.xml)；读取范围：1–150。
核对符号：`properties / dependencyManagement`。
Java 8；Maven 属性 3.9.16；Jackson BOM 2.18.9；JUnit 5.11.4；POM 4.0.0。
Git blob：`1b0b97325576536b8770db9946a44047dc67ebb5`。

### E14 — `pom.xml`
来源：[easy-4-java/opencli-java-sdk @ 26d703c72389](https://github.com/easy-4-java/opencli-java-sdk/blob/26d703c72389361e1b755733ac61145f783334b5/pom.xml)；读取范围：1–86。
核对符号：`properties`。
Java 17；Maven 属性 3.9.16；Jackson BOM 2.22.1；JUnit 6.1.0；POM 4.0.0。
Git blob：`13625e78cf40b1a5462695e4d8780a212200d842`。

### E15 — `pom.xml`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/pom.xml)；读取范围：1–150。
核对符号：`modelVersion / properties / dependencyManagement`。
Java 21；Maven 属性 4.0.0-rc-5；Jackson BOM 3.2.1 / tools.jackson；POM 4.1.0；description 尚写 2.x。
Git blob：`5dc8371f52b8f056de1cb07d9e0efb6cd653a836`。

### E16 — `.github/workflows/ci.yml`
来源：[easy-4-java/opencli-java-sdk @ a1278008c17f](https://github.com/easy-4-java/opencli-java-sdk/blob/a1278008c17fd94523ad47307183485fefdf67e0/.github/workflows/ci.yml)；读取范围：全文。
核对符号：`on / jobs.build`。
3.x workflow 仅针对该 feature 分支和手动触发；Ubuntu/JDK21/checked-in Maven wrapper；不能将规范分支无运行视为 CI 成功。
Git blob：`de848421bc39110d52112761445f305dd72fc5f4`。

### E17 — `src/main/java/io/github/easy4j/opencli/core/OpenCliExecutor.java`
来源：[easy-4-java/opencli-java-sdk @ 2095f617d567](https://github.com/easy-4-java/opencli-java-sdk/blob/2095f617d56789569d32cffe2cdedffe595270ba/src/main/java/io/github/easy4j/opencli/core/OpenCliExecutor.java)；读取范围：120–180。
核对符号：`invoke / appendCleanArgs`。
1.x 同样 trim 并丢弃空白参数，已在本轮独立读取。
Git blob：`f313578734adbefb58c94092a8e537adc6bb6963`。

### E18 — `src/main/java/io/github/easy4j/opencli/core/OpenCliExecutor.java`
来源：[easy-4-java/opencli-java-sdk @ 26d703c72389](https://github.com/easy-4-java/opencli-java-sdk/blob/26d703c72389361e1b755733ac61145f783334b5/src/main/java/io/github/easy4j/opencli/core/OpenCliExecutor.java)；读取范围：120–180。
核对符号：`invoke / appendCleanArgs`。
2.x 同样 trim 并丢弃空白参数；返回 Git blob 与 3.x 此文件相同。
Git blob：`ac1d8466dc9e7dc15e36cd2bdeb79fc2f8020661`。

### U01 — `package.json`
来源：[jackwener/OpenCLI @ 8271afc67e85](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/package.json)；读取范围：1–44。
核对符号：`version / exports`。
锁定 OpenCLI 1.8.8，./pipeline 是 JS package export，不足以证明存在公共 pipeline CLI。
Git blob：`988ca1b8d0df32cd31ee59c63f0327a4e35abf19`。

### U02 — `src/help.ts`
来源：[jackwener/OpenCLI @ 8271afc67e85](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/help.ts)；读取范围：1–240。
核对符号：`ArgSpec / OptionSpec / COMMON_OPTIONS / BROWSER_COMMON_OPTIONS`。
Structured Help 包含 required、optional value、negate、choices；公共参数含 format、trace、window、site-session、keep-tab。
Git blob：`409d69a38ff9d76bc2e9675aace757e4c2568851`。

### U03 — `src/help.ts`
来源：[jackwener/OpenCLI @ 8271afc67e85](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/help.ts)；读取范围：240–440。
核对符号：`commanderPath / commanderNamespaceHelpData`。
命名空间 Help 返回 command_options、namespace_options、global_options；browser 路径包含 <session> 占位段。
Git blob：`409d69a38ff9d76bc2e9675aace757e4c2568851`。

### U04 — `src/errors.ts`
来源：[jackwener/OpenCLI @ 8271afc67e85](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/errors.ts)；读取范围：1–250及245–文件末尾。
核对符号：`ErrorEnvelope / toEnvelope`。
错误模型为 ok:false/error；wire 字段是 help；包含 error.exitCode 以及可选 trace；hint 转为 help。
Git blob：`6f63a5baec6baa8333a4cb0911d310482ae73158`。

### U05 — `src/commanderAdapter.ts`
来源：[jackwener/OpenCLI @ 8271afc67e85](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/commanderAdapter.ts)；读取范围：1–225。
核对符号：`renderError / resolveExitCode`。
Adapter 错误通过 yaml.dump 后写 stderr，与成功数据 format 分开；可追加 # AutoFix 注释；进程 exit 与 envelope.exitCode 必须分别保留。
Git blob：`57e73fb4d2ce487080a68137dcf1711c12a7e728`。

### U06 — `src/pipeline/index.ts`
来源：[jackwener/OpenCLI @ 8271afc67e85](https://github.com/jackwener/OpenCLI/blob/8271afc67e8504bda94c147f446ee29775d08274/src/pipeline/index.ts)；读取范围：全文。
核对符号：`exports`。
导出 executePipeline、模板求值等 JS 能力；本轮未建立可供 Java subprocess 调用的公共 pipeline CLI 契约。
Git blob：`c9e1cd6cd8364c41ffdaa0a6f7a834ed432c2d62`。

## 工具与格式资料

- [CodeGraph CLI 官方参考](https://colbymchenry.github.io/codegraph/reference/cli/)：可独立使用 init、query、callers、callees、impact，不需要先有 MCP 注册。本轮由于工具未安装且 npm DNS 失败，未执行索引；不能称 grep 或源码阅读与 CodeGraph 等价。
- [OpenSpec 官方 README](https://github.com/Fission-AI/OpenSpec/blob/main/README.md)：proposal/design/specs/tasks、ADDED Requirements 与四级 Scenario。此链接为资料入口而非 SDK 兼容性锁定依据。
- [OpenSpec CLI 参考](https://github.com/Fission-AI/OpenSpec/blob/main/docs/cli.md)：官方 strict 校验应在可联网环境执行。本轮只运行本包的文档结构自检。

## 证据界限

本轮未拿到完整本地三分支 checkout，未重跑 Maven、真实 OpenCLI、CodeGraph 或远端 CI。未逐条验证 1275 个上游命令；不提供虚构覆盖率。正式实现必须补全真实进程 argv fixture、只读上游 CLI smoke 与所需 transport fixture。
