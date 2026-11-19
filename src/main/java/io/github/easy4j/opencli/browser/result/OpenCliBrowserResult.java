package io.github.easy4j.opencli.browser.result;

import io.github.easy4j.opencli.core.OpenCliResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** A strict browser result paired with the original process evidence. */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class OpenCliBrowserResult<T> {
    public enum Status { PARSED, TRUNCATED, EXECUTION_FAILED, EMPTY, INVALID_JSON, INVALID_SHAPE }

    private final OpenCliResult raw;
    private final Status status;
    /** Typed data, present only when status is PARSED. */
    private final T value;
}
