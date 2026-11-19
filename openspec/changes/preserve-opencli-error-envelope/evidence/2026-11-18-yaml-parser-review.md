# C03 YAML 解析器候选验证（2026-11-18）

C03 评审候选为 `org.yaml:snakeyaml:2.7`，三条 Java 维护线可共用。SnakeYAML Engine 的当前维护线要求 Java 11 或更新版本。[SnakeYAML](https://github.com/snakeyaml/snakeyaml) · [SnakeYAML Engine](https://github.com/snakeyaml/snakeyaml-engine) · [Maven Central 发布包](https://repo.maven.apache.org/maven2/org/yaml/snakeyaml/2.7/)

## 探针结果

独立探针使用 SHA-256 为 `2e194eba45a67dee19a4e272f4a04b18de8054e9f598b094382f6dae0b0e4b5e` 的 2.7 JAR。以 JDK 8 编译一次，在 8u504、17.0.21、21.0.13.0.2 各执行 30 个断言。三轮退出码均为 0。使用三个 [OpenCLI 1.8.8 真实 stderr 样本](2026-11-18-upstream-stderr.md) 及合成输入。探针未接入 SDK，SDK 当前行为仍由既有执行器决定。

- 三个真实样本均读出 `ok: false`、原始错误码及整数退出码。带 `# AutoFix` 的样本正常解析。合成 JSON 子集保留未识别的错误码和 trace 路径。普通单行文本得到字符串，后续 envelope 校验应按原执行状态处理。
- 普通 `SafeConstructor` 的 YAML 1.1 规则会将 `no`、`off` 和日期转成布尔值与日期类型。自定义隐式解析范围只接受 JSON 形式的布尔、空值、整数和小数。日期样式、`no`、`off` 和前导零字符串在探针中均保留原值。
- 默认浮点映射会将 `1e400` 转为无穷大，并对长小数取近似值。探针改用 `BigDecimal` 保留两者，后续模型应避免以浮点近似值覆盖扩展字段。
- 用库的事件流预检，拒绝锚点和别名、显式标记、多个文档及超过 32 层的集合。用加载选项拒绝重复键。UTF-8 字节检查在入口执行，加载器另外限制码点数。探针验证了 32 层和 256 KiB 边界、多字节超限、截断的 JSON 形态以及被禁止的 YAML 特性。普通运行日志仅记录断言数量。

## 实现评审范围

建议在 JSON 路径复用各分支已有 Jackson，并以严格 JSON 判定处理疑似 JSON 的失败输入。YAML 路径建议采用事件预检和受限构造，再检查 envelope 形状及字符串键。需用 SDK 测试验证退出码来源、解析状态、超时优先级、日志脱敏和无重试副作用。评审前保留 C03 任务 2.1 未勾选，三个 POM 均未引入该候选依赖。
