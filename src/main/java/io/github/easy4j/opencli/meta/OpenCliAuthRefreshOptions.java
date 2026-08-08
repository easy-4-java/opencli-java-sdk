package io.github.easy4j.opencli.meta;

import lombok.Builder;
import lombok.Data;

/**
 * {@link OpenCliAuthClient#refresh(OpenCliAuthRefreshOptions)} 的参数对象。
 *
 * <p>对应 {@code auth refresh}：限定 site（或 {@code --all} 全部刷新）、并发、超时与输出格式。</p>
 */
@Data
@Builder/**

 * Options for {@link OpenCliAuthClient#refresh(OpenCliAuthRefreshOptions)}.
 *
 * <p>Corresponds to {@code auth refresh}: restrict by site (or {@code --all}),
 * concurrency, timeout, and output format.</p>

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public class OpenCliAuthRefreshOptions {

    /** 限制的 site id（{@code --site}）。 */
    private String site;

    /** 刷新全部 site（{@code --all}），与 {@link #site} 互斥。 */
    private Boolean all;

    /** 并发刷新任务数（{@code --concurrency}）。 */
    private Integer concurrency;

    /** 单次刷新超时秒数（{@code --timeout}）。 */
    private Integer timeout;

    /** 输出格式（{@code -f}）：table、json、yaml、md、csv。 */
    private String format;
}
