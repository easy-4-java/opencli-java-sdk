package io.github.hiwepy.opencli.parser;

import lombok.Builder;
import lombok.Getter;

/**
 * 对 OpenCLI 输出的「尽力而为」轻量解析摘要。
 * <p>
 * 不同适配器可逐步扩展新字段；无法解析时所有字段可为 null，调用方应降级读取原始 stdout。
 * </p>
 */
@Getter
@Builder
public class OpenCliParsedFields {

    /**
     * 若能识别出的 JSON 负载根节点类型提示：array / object / text。
     */
    private final String jsonShapeHint;
}
