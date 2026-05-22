package io.github.hiwepy.opencli.browser.support;

import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * {@code browser screenshot} 附加选项（宽高、annotate、full-page）。
 */
@Data
@Builder
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
        if (Boolean.TRUE.equals(annotate)) {
            target.add("--annotate");
        }
        if (Boolean.TRUE.equals(fullPage)) {
            target.add("--full-page");
        }
        if (width != null) {
            OpenCliArgSupport.addOptionPair(target, "--width", String.valueOf(width));
        }
        if (height != null) {
            OpenCliArgSupport.addOptionPair(target, "--height", String.valueOf(height));
        }
    }
}
