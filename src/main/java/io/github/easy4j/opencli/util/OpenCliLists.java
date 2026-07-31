package io.github.easy4j.opencli.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JDK 8 兼容的不可变列表工厂（替代 {@link List#of}）。
 */
public final class OpenCliLists {

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
