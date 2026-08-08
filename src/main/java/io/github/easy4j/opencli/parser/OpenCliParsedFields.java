package io.github.easy4j.opencli.parser;

import lombok.Builder;
import lombok.Getter;

/**
 * 对 OpenCLI 输出的「尽力而为」轻量解析摘要。
 * <p>
 * 不同适配器可逐步扩展新字段；无法解析时所有字段可为 null，调用方应降级读取原始 stdout。
 * </p>
 */
@Getter
@Builder/**

 * Lightweight best-effort parse summary of OpenCLI output.
 *
 * <p>Different adapters can progressively extend new fields; when parsing fails,
 * all fields may be null and callers should fall back to reading raw stdout.</p>

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public class OpenCliParsedFields {

    /**
     * 若能识别出的 JSON 负载根节点类型提示：array / object / text。
     */
    private final String jsonShapeHint;
}
