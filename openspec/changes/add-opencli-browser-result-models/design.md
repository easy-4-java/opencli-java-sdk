# C07 Design · Browser 领域结果与严格解析

> DRAFT。以下为拟实施设计；不是当前库能力声明。

## Context

AX flags 和 JsonNode typed wrapper 已存在。真正的缺口是按 Browser leaf 的领域结果、解析失败语义与可追溯输出 fixture，而不是再造一组功能相同的参数。

证据：E03, E09, E12, U02, U03，见 [证据索引](../../../docs/architecture/opencli-java-sdk-evidence.md)。完整边界遵守 [目标架构](../../../docs/architecture/opencli-java-sdk-target-architecture.md)。

## Goals / Non-Goals

为高价值 Browser leaf 增加 strict decoder 和领域模型，保持现有 raw/JsonNode APIs。不假定所有输出都有统一 envelope，不自动关闭用户绑定标签页。

共同非目标：不重写站点浏览器实现，不自动执行真实账户写操作，不虚构官方远程 endpoint，不用格式通过代替代码验收。

## Decisions

### D1

第一批候选为 state、tab list、find、console、network、open/analyze；只有取得对应版本真实输出fixture的leaf才进入typed支持清单，缺证据项明确pending。

### D2

兼容方法签名继续返回现有OpenCliResult/JsonNode；增量typed方法或显式decoder提供严格结果。parseLenient不改名为严格解析。

### D3

按leaf验证数组/对象/文本、必填字段和空数据；保留unknown fields、raw与parse metadata，截断JSON不能静默映射为空对象。

### D4

tabId、requestId等标识符按真实wire类型保真；不经过double进行转换。若模型需字符串，保留原始JSON类型以便回放。

### D5

source=dom|ax与compareSources沿用已存在Options；新增枚举/约束须保留现有String迁移路径。close返回OpenCliResult，不能直接实现返回void的AutoCloseable。

## Alternatives considered

不删除JsonNode escape hatch，不凭help的输入schema猜所有输出DTO，不通过生成上千个模型掩盖缺少输出证据。

## Risks / Trade-offs

不同平台/版本的Browser输出会变化。任何字段的required/optional必须基于fixture和源码，缺少完整fixture的leaf不进入“已验证typed”名单。

## Migration plan

新增strict decoder和可选typed方法，raw与旧lenient行为保留且明确命名。AX参数本身不重复添加；状态枚举之外的future值通过raw兼容而不悄悄改默认。

每条版本线的迁移分开验证：Java8/17/21、Jackson2/3、各自Maven wrapper。保留现有公开门面，涉及失败行为收紧时提供调用示例与兼容边界。

## Validation strategy

Requirement IDs: OC-BROWSER-001, OC-BROWSER-002, OC-BROWSER-003, OC-BROWSER-004, OC-BROWSER-005, OC-BROWSER-006。

对应 [spec](specs/opencli-browser-result-models/spec.md) 的每个Scenario都必须有可追踪测试。先记录失败，再实现并复验；可控本地进程/HTTP/WS fixtures优先，不依赖用户账号。

提交验证记录至少包含目标ref、命令、tool版本、testcount、skipcount、退出码与artifact。所有未执行项保持NOT_RUN。

## Review decisions required before implementation

本设计的资源默认值、兼容行为与依赖选择均需规范评审；确认后再开始tasks。需要新的server wire协议、公共API破坏或未声明副作用时，应新增变更而不是在实现中静默扩大范围。
