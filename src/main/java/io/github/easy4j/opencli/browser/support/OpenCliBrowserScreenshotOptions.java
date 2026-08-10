package io.github.easy4j.opencli.browser.support;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * {@code browser screenshot} 附加选项（宽高、annotate、full-page）。
 */
@Data
@Builder/**

 * Options for {@code browser screenshot} (width, height, annotate, full-page).

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class OpenCliBrowserScreenshotOptions {

    private Boolean annotate;

    private Boolean fullPage;

    /** 覆盖视口宽度（CSS 像素，仅本次截图）。 */
    private Integer width;

    /** 覆盖视口高度（与 {@code --full-page} 互斥时 CLI 忽略 height）。 */
    private Integer height;

    /**
     * 追加到 argv。
     *
     * @param target 可变 argv 列表
     */
    public void appendTo(List<String> target) {
        OpenCliArgSupport.addFlagIfTrue(target, "--annotate", annotate);
        OpenCliArgSupport.addFlagIfTrue(target, "--full-page", fullPage);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--width", width);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--height", height);
    }
}
