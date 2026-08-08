package io.github.easy4j.opencli.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JDK 8-compatible immutable list factory (replacement for {@link List#of}).
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */public final class OpenCliLists {

    private OpenCliLists() {
    }

    /**
     * @param elements 元素序列，可为空
     * @param <T>      元素类型
     * @return 不可变列表
     */
    @SafeVarargs
    public static <T> List<T> of(T... elements) {
        if (elements == null || elements.length == 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(elements));
    }
}
