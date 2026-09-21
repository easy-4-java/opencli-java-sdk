# C10 基础与 C01 / C02 第一批实施记录

> 2026-09-21。状态：IMPLEMENTATION_IN_PROGRESS / REVIEW_REQUIRED。
> 本文记录实际实现和实际验证，不代表十个 Change 已完成，也不关闭三分支集成门禁。

## 1. 执行顺序与分支边界

顺序保持：C10 共享测试基础 → C01/C02/C03/C04/C05/C09 → C06/C07/C08 → C10 最终集成。

标准实现线为 `feature/2.0.x`。本批在隔离分支 `feature/2.0.x-contract-hardening` 实施，基点为 `d0c8056990f7a47fcc202acffa387ba066bcfc67`。1.x 基点 `abba809f11dae68437c39d2ea5a2f4cf8798c0ef`，3.x 基点 `6e38904bdfcae90ec617e8d29bf3d8cf2f002893`。共享 fixture 的 sources.lock.json 保存三线 Java/Jackson/Maven 允许差异。当前实施没有移动这三个原始 feature 分支，也没有修改 main。

本批未开始 C03/C04/C05/C06/C07/C08 的新实现；移除本地 Executor/Adapter 默认参数日志只是 C09 的局部改进，不等于整个 SDK 的日志/异常隐私治理完成。

## 2. 实际 RED → GREEN

| 阶段 | 精确提交 | 验证结果 | GitHub Actions run |
|---|---|---|---|
| C01 原始参数 RED | `94de053f4ae7a0912201c2bbeab81b8332f660b8` | 全套 2027 项；21 failures、0 errors、0 skipped。新增 33 项参数契约中 21 项失败 | [35562286431](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/35562286431) |
| SDK 层修复后的剩余 RED | `c93a25c182f0b0acd15cdfa01089599b52601854` | 2028 项；仍有 11 项失败，原因在 Commons Exec 内部 trim | [35562634249](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/35562634249) |
| 原始参数 GREEN | `6f830b4f2e5b87bb57eb6b3b669fd299ee48ee39` | clean verify、参数契约门禁、官方 strict、报告上传通过 | [35562782412](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/35562782412) |
| 有序选项/快照 RED | `d02b16ddc5e0fe7c1d98c6847f4b2999cf2f2566` | 2038 项；10 failures、0 errors、0 skipped；原始 33 项参数契约仍通过 | [35562952517](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/35562952517) |
| 有序选项/快照 GREEN | `48b14388bc1c86fcf1dff2705f0140a541a3fa75` | 完整验证与官方 strict 通过 | [35563156619](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/35563156619) |
| C02 资源行为 RED | `31bde9cc4bda23519b6a81d66df7ddb781d94548` | 2046 项；新增八项全部失败；0 errors、0 skipped | [35563763025](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/35563763025) |
| 执行核 GREEN | `98e3db1f8883b8485ec8a428238173dc1148dfb4` | 完整验证、资源回归、官方 strict 通过 | [35564181728](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/35564181728) |
| 扩展边界 GREEN | `3bacc8d09b1eeeaa05d6137e2a7e9c350d6622ce` | Surefire XML 实际合计 2052 项，0 failures、0 errors、0 skipped；argv 43 项、process 14 项 | [35564455068](https://github.com/easy-4-java/opencli-java-sdk/actions/runs/35564455068) |

初始 `cdc8b66` 的 CI 因 Wrapper JAR 缺失、隐藏证据目录未被上传而失败；它不是有效行为 RED。修复构建基础后才得到表中的真实失败证据。2.x Wrapper 复用仓库已有的 launcher JAR，保留 Maven 3.9.16 分发；未修改 POM 依赖版本或降低 JaCoCo 门槛。

## 3. C10 已建立的基础

`src/test/resources/opencli-contracts/v1/` 包含版本化的 UTF-8/base64 参数向量、内容 hash、来源 lock、三线基点和允许差异。向量明确标为 synthetic offline，不伪装成上游或网站采集结果。TSV 最后一列可以为空；限定到该文件的 .gitattributes 保留末尾分隔符，不允许格式化器把空参数删除。

`ContractProbe` 输出真实 JVM 参数，覆盖 Executor List/varargs、Adapter List/varargs、结构化 request 与 Browser fill 路径。不是只断言 Recording executor 的前两项。

`scripts/contract_report.py` 分开记录 enumeration、argv、process、protocol、typed-result、real-execution。缺报告、零测试、required suite 被跳过、实际 Maven exit 非零、坏 XML、声明数量与实际 testcase 不一致均失败。13 项 Python 检查器测试经过本地 RED→GREEN，并在 CI 再次运行。

NOT_RUN 表示本轮没有为该层选择新的契约证据，不表示既有协议/解析测试被跳过；完整 Maven 套件仍运行全部既有测试。Java 子进程探针不等于 live OpenCLI/网站验证。

`.github/workflows/contracts.yml` 使用分支对应 JDK 与 checked-in Wrapper，记录精确 HEAD、实际 Java/Maven 版本、退出码、Surefire XML、JaCoCo 和分层报告。所有十个 Change 均使用官方 OpenSpec 1.13.1 逐项及 `--all --strict --no-interactive` 校验；在上述绿色 run 中均通过。

## 4. C01 的实现内容和兼容约束

参数值原样保留空字符串、空白、换行、Unicode、引号、`--` 和含 `=` 的内容。null 元素按字段和索引报错，不在错误中拼接其它参数值。命令标识符与值分开校验；输入在排队之前做快照。

Commons Exec 的 Argument 构造器在 handleQuoting=false 时仍 trim；先由 LiteralCommandLine 保留原始向量，当前执行核再把完整向量直接交给 ProcessBuilder。没有自动 shell 展开或拼接 shell 字符串。

OpenCliOptionSchema / OpenCliOption 增量支持带值选项、可重复有序 occurrences、显式 false 和否定 flag，拒绝 schema 冲突与 legacy/ordered 重叠。旧 Map 中 Boolean 仍保持历史 presence-only 行为；不能把旧 false 静默改成否定 flag。Map 和可变对象的常规值在构造时捕获。

Windows `.cmd/.bat` 被明确拒绝，调用方应使用原生 node 可执行文件及 JS 路径；Windows/macOS 实际执行尚未验证。legacy appendQuotedKeyValue 保留，不作为新的 literal 路径使用。

## 5. C02 的实现内容和边界

每个 Executor 默认持有稳定的容量 owner；显式共享 OpenCliProcessRuntime 才跨 Client 共用许可。0 使用 CPU 派生默认值，负数拒绝。旧静态 bridge 仅保留给兼容调用，且在存在提交时拒绝重配；构造新 Client 不再修改它。

System.nanoTime 驱动提交到执行的总预算。排队到期不创建子进程；启动前取消也不创建子进程。stdout 默认捕获 8 MiB，stderr 2 MiB；超过预算返回 OUTPUT_LIMIT 失败和有界部分输出。字段分别记录 retained/observed 字节数与截断标记；UTF-8 截断采用替换字符解码，字节计数不根据重新编码后的文本计算。

清理只有有限 grace，默认 5 秒；中断标记会恢复，许可在收尾后释放。主进程或读取线程未确认结束时，runtime 被隔离，不继续积累新的不确定资源。结果携带 terminationReason、cleanupState、processStarted、streamsDrained、字节计数和 elapsed/queue 时间。

主进程确认退出不等于所有后代退出。当前 portable backend 固定保留 descendantsExitConfirmed=false；不按进程名/PID 批量终止共享 daemon 或用户浏览器。同步 follow 仍受有限 timeout/capture 约束；没有实现无限流式 sink 或远端取消协议。

## 6. 尚未满足的关闭条件

C01/C02 的 Change 继续 IN_PROGRESS；不能因为上述测试通过就整体勾选或 archive。

- C01：自定义 Object.toString() 返回 null 的输入应补专门拒绝测试；Windows/macOS 和三版本线实际完整回归尚未完成。
- C02：底层 stop/destroy 抛 unchecked 异常时的收尾保护需要故障注入验证和补强；未知清理、取消/自然退出竞争及未确认后代的验收仍需继续。
- C09：HTTP、WS、诊断和异常 cause 链仍需独立隐私回归；本地参数日志减少不构成全域完成。
- C03/C04/C05 尚未进入本轮新实现，C06/C07/C08 也尚未进入；不得跳过这些项关闭 C10。
- 1.x/JDK8、3.x/JDK21、真实 OpenCLI 输出采集、CodeGraph 索引以及独立代码评审未完成。不能用本批 JDK17 证据代替它们。

## 7. 实际产物验证

扩展边界 run 35564455068 的 JDK artifact SHA-256：`86cc81f83792b5c65d648c04db1afee6970f46335583c7ad8ff2cdb34b6f4897`。
其中 committed-source tar SHA-256：`c11fe761f44af076f29dffe8aa10f3000c9d1d421bcfc83aa2191da4afe08d35`。
该 tar 在容器重建的 Git tree 精确等于 `f421af093023fb11683fcd4334806f12d9aa140c`，与 3bacc8d 的远端树一致；这是固定源码快照，不是声称已 clone 完整 Git 历史或同步用户 Mac。

官方 strict artifact SHA-256：`aac29fdd4c5c3f7c9c556e7a92036d4956c630609ad5a9f37ddd25dfc1c24a24`；all.log 实际为 10 passed / 0 failed，version.txt 为 1.13.1。

后续提交会使 HEAD 改变，最终状态应读取该精确 HEAD 的新 CI，而不是重复使用旧绿色结果。
