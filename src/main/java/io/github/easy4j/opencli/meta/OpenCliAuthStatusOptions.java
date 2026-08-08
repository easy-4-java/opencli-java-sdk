package io.github.easy4j.opencli.meta;

import lombok.Builder;
import lombok.Data;

/**
 * {@link OpenCliAuthClient#status(OpenCliAuthStatusOptions)} 的参数对象。
 *
 * <p>对应 {@code auth status}：限定 site、显示完整会话、并发与超时、{@code --only <status>} 输出过滤、
 * 与 {@code -f <fmt>} 输出格式。</p>
 */
@Data
@Builder/**

 * Options for {@link OpenCliAuthClient#status(OpenCliAuthStatusOptions)}.
 *
 * <p>Corresponds to {@code auth status}: restrict by site, show full session info,
 * concurrency/timeout, {@code --only <status>} output filtering, and {@code -f <fmt>} output format.</p>

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public class OpenCliAuthStatusOptions {

    /** 限制的 site id（{@code --site}），{@code null} 表示全部。 */
    private String site;

    /** 打印完整会话信息（{@code --full}），{@code null}/false 时不追加。 */
    private Boolean full;

    /** 探测并发数（{@code --concurrency}）。 */
    private Integer concurrency;

    /** 单次探测超时秒数（{@code --timeout}）。 */
    private Integer timeout;

    /** 仅输出某字段（{@code --only}），例如 {@code status}、{@code expired}。 */
    private String only;

    /** 输出格式（{@code -f}）：table、json、yaml、md、csv。 */
    private String format;
}
