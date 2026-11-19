package io.github.easy4j.opencli.browser.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Sources accepted by the typed browser snapshot entry point. */
@Getter
@RequiredArgsConstructor
public enum OpenCliBrowserSnapshotSource {
    DOM("dom"),
    AX("ax");

    private final String optionValue;
}
