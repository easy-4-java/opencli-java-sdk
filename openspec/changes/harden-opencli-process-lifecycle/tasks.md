# C02 Tasks · 有界执行、排队超时与进程清理

> 已勾选任务有对应实现和测试证据。

关联规范：[spec](specs/opencli-process-lifecycle/spec.md)。

## 1. 资源回归

- [ ] 1.1 用可控子进程分别编写排队超时、并发 Client、取消、超限与 cleanup 竞态失败测试。
- [ ] 1.2 测量在途进程与 capture 峰值；保留失败证据而不运行真实用户网站。

## 2. 执行核修复

- [x] 2.1 引入稳定 limiter 和单调 deadline，将许可等待纳入总预算。
- [x] 2.2 实现有限 capture、OUTPUT_LIMIT 和安全 UTF-8 截断策略。
- [ ] 2.3 实现统一终结路径与有限 cleanup，保护共享 daemon/用户进程。

## 3. 兼容和证据

- [ ] 3.1 明确 0/负数配置语义、总预算与 cleanup grace、同步 follow 限制。
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
