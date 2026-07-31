package io.github.easy4j.opencli.meta;

import lombok.Builder;
import lombok.Data;

/**
 * {@link OpenCliSkillsClient#read(OpenCliSkillsReadOptions)} 的参数对象。
 */
@Data
@Builder
public class OpenCliSkillsReadOptions {

    /** 技能包 id，例如 {@code opencli-browse}。 */
    private String skill;

    /** 可选的子路径片段（{@code null} 表示读取整体）。 */
    private String path;

    /** {@code --json} 开关：true 时输出 JSON。 */
    private Boolean asJson;
}
