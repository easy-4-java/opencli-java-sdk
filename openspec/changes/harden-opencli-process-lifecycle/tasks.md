# C02 Tasks · 有界执行、排队超时与进程清理

> 全部为待执行实现任务。文档/格式检查通过不允许提前勾选。

关联规范：[spec](specs/opencli-process-lifecycle/spec.md)。

## 1. 资源回归

- [ ] 1.1 用可控子进程分别编写排队超时、并发 Client、取消、超限与 cleanup 竞态失败测试。
- [ ] 1.2 测量在途进程与 capture 峰值；保留失败证据而不运行真实用户网站。

## 2. 执行核修复

- [ ] 2.1 引入稳定 limiter 和单调 deadline，将许可等待纳入总预算。
- [ ] 2.2 实现有限 capture、OUTPUT_LIMIT 和安全 UTF-8 截断策略。
- [ ] 2.3 实现统一终结路径与有限 cleanup，保护共享 daemon/用户进程。

## 3. 兼容和证据

- [ ] 3.1 明确 0/负数配置语义、总预算与 cleanup grace、同步 follow 限制。
- [ ] 3.2 在三线和声明支持的 OS 上验证真实进程资源回收；能力缺口不得标绿。
- [ ] 3.3 回归现有超时/UTF-8/异常测试，记录 HEAD、数量、跳过项和退出码。

## 证据交付模板（实施时填写）

每项任务的完成记录包含：Requirement/Scenario、分支HEAD、测试命令、失败→通过证据、测试数/跳过数、退出码、报告位置。没有记录的任务保持未完成。

## OC-PROC-006 本地同步采集验证

`OpenCliProcessContractTest` 经 typed `browser network --follow` 入口运行真实 JVM fixture，校验 CLI argv，并覆盖正常退出、CLI 非零退出、输出超限、调用线程中断、执行超时，以及禁用时限时启动前拒绝。超时测试同时启动独立进程，确认该进程仍运行并可自行正常退出。JDK 8 执行 `mvn -B -ntp -o clean verify` 退出 0，全量 2,095 项（其中该类 14 项），0 失败、0 错误、0 跳过。

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

JDK 8 使用系统 Maven 执行 `-B -ntp -o clean verify`，2,114 项通过，0 失败、0 错误、0 跳过。真实 OpenCLI 1.8.6 两项复验通过。4,500 ms 时限返回 `EXECUTION_TIMEOUT` 并保留 34 条完整事件。2 KiB 上限返回 `OUTPUT_LIMIT` 并保留 2,048 字节和 11 条完整事件。两次均确认 SDK 启动的 CLI 子进程已退出，后续 daemon 查询可用。

本次测量单个捕获数组的容量。整体堆内存峰值还包含扩容时的数组复制、输出快照和并发调用，任务 1.2 继续跟踪。
