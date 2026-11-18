# C06 Tasks · 结构化能力发现与可追溯快照

> 勾选项有对应的执行证据。

关联规范：[spec](specs/opencli-capability-discovery/spec.md)。

## 1. schema基线

- [x] 1.1 从锁定 OpenCLI 采集 root、browser、meta 和 site 帮助，保存原文与 SHA-256。不能读取为单一 JSON 文档的输出单列为负例。见 [1.8.8 采集证据](evidence/2026-11-19-structured-help.md)。
- [ ] 1.2 为scope、optionalvalue、negate、false、aliases、session占位和旧格式编写失败测试。

## 2. 服务实现

- [ ] 2.1 新增Java8可表达schema与快照模型，保留unknown字段和raw；复用已有adapter入口。
- [ ] 2.2 实现有限version/help探测与可信executable校验，禁止自动业务操作。
- [ ] 2.3 实现cache key、内容hash、失效与UNKNOWN/STALE/UNSUPPORTED原因。

## 3. 边界验证

- [ ] 3.1 测试本地override/插件来源未知时不伪造来源和全量覆盖率。
- [x] 3.2 验证发现失败后仍可调用本地 raw 入口，远程拒绝范围继续由 C04 测试验收。
- [ ] 3.3 三线同一schema fixture结果一致；报告未运行的真实上游采集项。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。


## OC-CAP-001/002/005 结构化帮助解析

`OpenCliHelpParser` 在 Java 8 API 范围内提供帮助解析结果。根目录、站点、命名空间和叶子按真实 1.8.8 结构识别。Schema 保留命令显示路径、会话占位符、命令层级、位置参数、别名和分作用域选项。缺失字段、显式 false、可选取值、否定和默认值分别保留。原文和各节点的来源 JSON 可以追溯未解释字段。纯文本、JSON 后缀、重复字段或无效结构返回 `UNKNOWN` 及对应原因。

基线的 API 验收先因缺少解析入口而无法编译。加入实现后，以 1.8.8 的七个 JSON 样本、两个原始负例和合成扩展验证，详见 `OpenCliHelpParserTest`。当前验证范围为原文解码，运行时探测、快照来源与失效、类型化会话绑定仍由其余任务验收。

OC-CAP-005 控制字符回归：在 JDK 17 基线 `1ff6377` 上，合法 JSON 后带 NUL 的用例返回 `AVAILABLE`，定向测试失败。解析器现按 JSON 规定识别四种前导空白，并把完整原文交给严格 JSON 解析。首尾出现 NUL 或只有 NUL 均返回 `UNKNOWN/INVALID_JSON`，原文仍可由调用方显式取得。空格、制表符、回车和换行仍能包围合法文档。

对 `af62b29`、`1ff6377`、`6a50081` 应用同一修复后，分别用 Java 8、17、21 执行 `./mvnw -B -ntp clean verify`，退出码均为 0。每条线五项解析测试全部执行，全量测试数分别为 2186、2187、2186，均为零失败、零错误、零跳过。GitHub 契约报告将解析用例计入枚举层，与帮助样本形态测试共同验收。

OC-CAP-005 根目录形态与本地回退：JDK 17 基线 `b22796f3` 上，空注册表对象和空参数名均被错误标记为 `AVAILABLE`，两项定向测试失败。根据 1.8.8 的根帮助样本，根注册表要求 `site_adapters.sites` 为数组，成员为非空白名称。显式空数组仍是合法的空目录。参数名和别名必须包含有效字符，选项候选值保留显式空字符串。实际子进程测试返回缺少 sites 的根帮助，发现结果为 `UNKNOWN/INVALID_SCHEMA`，随后同一客户端成功执行本地 raw 调用。远程模式的发现仍返回 `LOCAL_DISCOVERY_ONLY`，C04 远程契约用例随全量构建一并执行。

使用各条线的 Wrapper 执行 `./mvnw -B -ntp clean verify`，Java 8、17、21 分别通过 2194、2195、2194 项。每条线均执行 7 项解析测试和 6 项发现测试，零失败、零错误、零跳过。报告位于对应构建目录的 `target/surefire-reports`。

## OC-CAP-004/005/006 本地根发现

`OpenCliClient.discoverCapabilities()` 接受调用者信任的绝对本地可执行文件配置，记录规范路径、观测版本、采集时刻、配置指纹及完整帮助文本的 SHA-256。探测固定为 `--version` 和 `--help -f json`，复用客户端执行器的本地并发容量、队列和进程清理。每次探测的队列与运行合计最多 10 秒，清理最多 1 秒，每路最多保留 1 MiB，调用者设置的更小预算优先。命令来源为 `UNKNOWN`，观察范围为 `ROOT_HELP`。初始实现采用按需重新观测，同版本改动可形成不同帮助摘要。后续快照失效和 `STALE` 行为见下文。远端和 PATH 查找的发现请求返回 `UNSUPPORTED`，原有显式本地调用仍可使用。

`OpenCliCapabilityDiscoveryTest` 先因缺少公开发现 API 无法编译。真实 JVM 子进程验收固定参数、同版本变更、无来源推断、文本降级、输出超限、非零退出、超时及 local raw。Maven 定向测试同时验证原有帮助解析。范围仍为根级目录，细粒度帮助探测及类型化执行仍未验收。

## OC-CAP-004/005 根帮助快照失效

客户端保存一个最近成功的根帮助快照，每次显式发现均重新探测。刷新失败且配置指纹一致、未观测到不同版本时，返回 `STALE`，保留成功时的采集时间、schema 和帮助摘要。失败的刷新另存于 `failedRefresh`，包括失败原因、尝试时间和可取得的原始帮助。配置或已观测版本改变会清除旧快照；成功刷新以新内容更新摘要。同一客户端并发探测时，较早发起但较晚完成的探测不会覆盖已发布的新结果。

`OpenCliCapabilityDiscoveryTest` 的定向红灯为预期 `STALE`、实际 `UNKNOWN`。实现后验证同配置降级、失败原文、同版本帮助变化、配置与版本失效、并发完成顺序。当前缓存范围为根帮助。

## OC-CAP-001/003/004/005/006 固定范围帮助探测

`discoverCapabilities(OpenCliHelpScope)` 提供根目录、浏览器、浏览器 tab/get/state、auth、list 及 npm 站点和 search 的九个固定查询。细粒度参数只在已验证的 OpenCLI 1.8.8 启用。浏览器查询使用固定的帮助会话占位，不接受调用者提供的命令路径，也不执行帮助描述中的内容。结构化结果需匹配请求范围；纯文本和混合输出维持原有失败原因。

每个范围只保存其最近成功的观察，且配置或已观测版本变化使其他不匹配的范围一并失效。OpenCLI 能力仍按请求观察，并保留来源未知标记。增加 `OpenCliCapabilityDiscoveryTest` 的进程验收后，缺少范围 API 时失败，加入实现后定向测试通过。类型化会话绑定及执行前参数校验仍待验收。

### OC-CAP-006 版本探测输出校验

真实 JVM 子进程在版本号后输出 NUL 和换行。JDK 17 基线 `532cf673` 的定向回归检测到后续帮助进程被启动，退出码为 1。修复后仅允许空格、制表符、回车和换行包围版本号。遇到 NUL 时返回 `UNKNOWN/INVALID_VERSION_OUTPUT`，观测版本为空，且不启动帮助进程。

在固定范围帮助探测基线 `8d1ccb6`、`a6b59b54`、`e83bc00` 上应用修复后，分别用 Java 8、17、21 执行 `./mvnw -B -ntp clean verify`。三个构建均退出 0，各运行 12 项发现测试。全量测试数分别为 2200、2201、2200，零失败、零错误、零跳过。报告见 `target/surefire-reports`。
