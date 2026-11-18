# C02 Tasks · 有界执行、排队超时与进程清理

> 已勾选任务有对应实现和测试证据。

关联规范：[spec](specs/opencli-process-lifecycle/spec.md)。

## 1. 资源回归

- [x] 1.1 用可控子进程分别编写排队超时、并发 Client、取消、超限与 cleanup 竞态失败测试。见本文 OC-PROC-004/005 回归矩阵。
- [x] 1.2 测量在途进程与 capture 峰值；保留失败证据而不运行真实用户网站。见本文 OC-PROC-001/002/003 饱和验证。

## 2. 执行核修复

- [x] 2.1 引入稳定 limiter 和单调 deadline，将许可等待纳入总预算。
- [x] 2.2 实现有限 capture、OUTPUT_LIMIT 和安全 UTF-8 截断策略。
- [ ] 2.3 实现统一终结路径与有限 cleanup，保护共享 daemon/用户进程。

## 3. 兼容和证据

- [x] 3.1 明确 0/负数配置语义、总预算与 cleanup grace、同步 follow 限制。
- [ ] 3.2 在三线和声明支持的 OS 上验证真实进程资源回收；能力缺口不得标绿。
- [x] 3.3 回归现有超时/UTF-8/异常测试，记录 HEAD、数量、跳过项和退出码。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## 2026-11-18 三线验证

OC-PROC-001/002 使用实际子进程验证独立及共享许可、排队超时前不启动进程。总预算实现使用 `System.nanoTime()`。OC-PROC-003 验证 stdout 和 stderr 的独立字节上限、超限部分结果、UTF-8 截断及反向执行器复制配置。Watchdog 故障注入和并发终结测试补充了 OC-PROC-004/005 的证据。

| JDK | 功能分支代码提交 | 验证 |
| --- | --- | --- |
| 8 | `3e1bfb3531ace72a504237bfc2ec838b92f9dc75` | 旧执行器的 `OpenCliProcessContractTest` 8 项失败。回移后 `mvn -B -ntp -o clean verify` 退出 0，2,087 项通过，0 失败、错误或跳过。[CI](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44307253778) |
| 17 | `5e6a36c68d2bfa34fc322190ca398bda717c78f5` | Watchdog 定向红灯 2 项失败，修复后定向 18 项通过。`mvn -B -ntp -o clean verify` 退出 0，2,088 项通过，0 失败、错误或跳过。[CI](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44306649188) |
| 21 | `a9d690675e1e7b5febab12eea2b17cfca77de4de` | 旧执行器的 `OpenCliProcessContractTest` 8 项失败。回移后 `bash ./mvnw -B -ntp -o clean verify` 退出 0，2,087 项通过，0 失败、错误或跳过。[CI](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/44307253570) |

各线的 JaCoCo 90% 告警门槛保留。完整后代所有权与回收证明、声明支持的 OS 实机矩阵、持续输出策略和独立评审仍待完成。


中英文 README 已对齐当前本地执行器的配置默认值、非法值、排队与执行共用预算、独立清理等待，以及同步 `--follow` 的限制。配置边界由 `OpenCliProcessBoundaryTest` 和 `OpenCliProcessContractTest` 验证。连续输出策略完整验收仍跟踪 OC-PROC-006。

## OC-PROC-006 本地同步采集验证

`OpenCliProcessContractTest` 经 typed `browser network --follow` 入口运行真实 JVM fixture，校验 CLI argv，并覆盖正常退出、CLI 非零退出、输出超限、调用线程中断、执行超时，以及禁用时限时启动前拒绝。超时测试同时启动独立进程，确认该进程仍运行并可自行正常退出。JDK 17 执行 `mvn -B -ntp -o clean verify` 退出 0，全量 2,099 项（其中该类 14 项），0 失败、0 错误、0 跳过。

## 2026-11-18 OC-PROC-006 真实 OpenCLI 验证

使用 macOS 27.0.1 ARM64、Node 24.18.0、OpenCLI 1.8.6、Chrome for Testing 158.0.8127.18 和 [Browser Bridge 1.0.22](https://github.com/jackwener/OpenCLI/releases/tag/v1.8.6)。扩展归档的 SHA-256 为 `9d2e3d053948beab5d97124aa79b1532d2122e33e461eca56cac113afd33207a`。每条维护线使用新的临时浏览器配置和显式 profile 路由。页面和数据接口仅绑定回环地址，每 120 ms 发起一次带序号的请求。

SDK 通过 `OpenCliBrowserSession.network` 设置 `follow(true)` 和 `all(true)`，实际运行 `opencli --profile <id> browser <session> network --follow --all`。探针分别使用 4,500 ms 时限和 64 KiB 上限，以及 12,000 ms 时限和 2,048 字节上限。每次解析部分 stdout 中完整的 JSON 行，确认来自测试接口、HTTP 状态为 200，且至少有两个不同请求。每条维护线用对应 JDK 执行 `test-compile`，JDK 21 使用仓库 Maven Wrapper。

| JDK | 参与验证的功能分支提交 | 执行超时，完整事件 / 捕获字节 / 耗时 | 输出超限，完整事件 / 捕获字节 / 观测字节 / 耗时 |
| --- | --- | --- | --- |
| 8 | `293c281a93c22a077d0f6a9b5ba2663bd6a6ea38` | 34 / 6,044 / 4,519 ms | 11 / 2,048 / 2,844 / 2,220 ms |
| 17 | `d22a1937ca43f03cb17a707710fb8899fa963b6d` | 34 / 6,044 / 4,524 ms | 11 / 2,048 / 2,132 / 2,204 ms |
| 21 | `1ed41f928b4c906cb0499955687d33270c70c2ca` | 33 / 5,866 / 4,518 ms | 11 / 2,048 / 2,844 / 2,208 ms |

6 个场景通过，0 失败、0 跳过。SDK 分别返回 `EXECUTION_TIMEOUT` 和 `OUTPUT_LIMIT`，均附带部分输出和 `ROOT_EXIT_CONFIRMED`。每组验证确认共享 daemon 的 PID 和隔离浏览器进程在两次调用后仍存活，后续标签页查询仍成功。当前结果验证 macOS 上的同步有界采集。受控 sink、后代进程所有权和其他操作系统上的真实浏览器验收仍待验证。

## 2026-11-18 OC-PROC-003 捕获数组扩容

JDK 17 红灯测试以 8,193 字节作为捕获上限，分两次写满，观察到旧实现把 backing array 扩到 16,384 字节。修复对单次分配容量应用捕获上限。回归验证 8,193 字节数组、原样保留的前缀、准确的观测字节数及截断标记。

JDK 17 使用系统 Maven 执行 `-B -ntp -o clean verify`，2,115 项通过，0 失败、0 错误、0 跳过。真实 OpenCLI 1.8.6 两项复验通过。4,500 ms 时限返回 `EXECUTION_TIMEOUT` 并保留 33 条完整事件。2 KiB 上限返回 `OUTPUT_LIMIT` 并保留 2,048 字节和 11 条完整事件。两次均确认 SDK 启动的 CLI 子进程已退出，后续 daemon 查询可用。

本次测量单个捕获数组的容量。整体堆内存还包含扩容时的数组复制、输出快照和并发调用，按独立指标记录。

## 2026-11-18 OC-PROC-001/002/003 饱和验证

`OpenCliProcessContractTest#sharedRuntimeBoundsConcurrentChildrenAndCombinedCapture` 使用三个客户端共享两个执行许可。两个真实 JVM 子进程分别写入 131,073 字节 stdout 和 65,539 字节 stderr，然后发出就绪标记并等待同一释放信号。两个子进程仍在途时提交四次调用，四次均返回 `QUEUE_TIMEOUT`，执行详情和子进程标记均确认没有启动进程。该场景在途峰值为 2。

释放后，两次调用均确认根进程退出，四条输出流保留 393,224 字节，恰好达到两个执行槽的合计捕获预算，零截断。这个数值由执行详情测得，对应离线 fixture 的捕获量。JVM 整体堆内存还包含其他分配，本场景不对其计量。

JDK 17 使用系统 Maven 执行 `-B -ntp -o clean verify`，2,116 项通过，0 失败、0 错误、0 跳过。

## 2026-11-18 OC-PROC-004/005 回归矩阵

`OpenCliProcessContractTest` 使用真实子进程覆盖排队超时、独立和共享 Client 限流、取消及独立流超限。`SubprocessCompletionRaceTest` 用同步屏障覆盖输出超限、取消、watchdog 与进程结束竞争，确认唯一结果和许可复用。`SubprocessCleanupFaultTest` 注入 watchdog 初始化、流初始化、终止和清理异常，覆盖最初失败保留及无法确认退出的运行时隔离。

新增真实继承管道回归。根进程启动继承 stdout、stderr 的独立 JVM，后者通过文件汇报心跳。观察到后代进程启动后取消调用。清理宽限配置为 250 ms，测试允许 1,500 ms 等待窗口，包含线程调度。SDK 仍返回 `CANCELLED`，流未排空时必须报告 `UNCONFIRMED`，并且不声称后代已退出。断言取消后后代心跳仍更新，最后由测试自己的释放文件终止它。

JDK 17 使用系统 Maven 执行 `-B -ntp -o clean verify`，2,117 项通过，0 失败、0 错误、0 跳过。该场景验证未追踪后代的安全边界，后代所有权和回收能力仍由 2.3、3.2 跟踪。

## 2026-11-18 OC-PROC-003 输出缓冲区按需分配

修复前，构造捕获对象即分配每个输出流的初始数组，默认两个共 16 KiB，包括仍排队或在启动前取消的调用。JDK 17 的两项回归分别观测到预取消路径的 16 字节测试配置和普通空写路径的 8,192 字节数组容量，预期均为零。

修复后，第一次非空写入才分配对应捕获数组，仍受流的配置上限约束。预取消路径的 stdout、stderr 容量均为零，读取线程的工作缓冲和 JVM 总堆内存属独立指标。红灯 2 项失败，修复后 JDK 17 定向 33 项通过。

JDK 17 使用系统 Maven 执行 `-B -ntp -o clean verify`，2,118 项通过，0 失败、0 错误、0 跳过。
