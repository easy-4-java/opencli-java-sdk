package io.github.easy4j.opencli.browser.support;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * {@code browser drag} 的前缀语义定位（{@code --from-role} 等）。
 */
@Data
@Builder
public class OpenCliBrowserDragLocator {

    private String fromRole;
    private String fromName;
    private String fromLabel;
    private String fromText;
    private String fromTestId;

    private String toRole;
    private String toName;
    private String toLabel;
    private String toText;
    private String toTestId;

    /**
     * @param target argv 列表
     */
    public void appendTo(List<String> target) {
        appendPrefixed(target, "from", fromRole, fromName, fromLabel, fromText, fromTestId);
        appendPrefixed(target, "to", toRole, toName, toLabel, toText, toTestId);
    }

    private static void appendPrefixed(
        List<String> target,
        String prefix,
        String role,
        String name,
        String label,
        String text,
        String testId) {
        OpenCliArgSupport.addOptionPairIfPresent(target, "--" + prefix + "-role", role);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--" + prefix + "-name", name);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--" + prefix + "-label", label);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--" + prefix + "-text", text);
        OpenCliArgSupport.addOptionPairIfPresent(target, "--" + prefix + "-testid", testId);
    }
}
