package io.github.easy4j.opencli.browser.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** The URL header and exact snapshot body from browser state. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserSnapshot {
    private final String url;
    private final String text;
}
