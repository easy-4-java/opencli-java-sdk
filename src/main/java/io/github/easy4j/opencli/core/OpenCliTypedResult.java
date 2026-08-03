package io.github.easy4j.opencli.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 将 {@link OpenCliResult} 与适配器专属结构化视图绑定。
 *
 * @param <T> 结构化负载类型
 */
@Getter
@RequiredArgsConstructor
public final class OpenCliTypedResult<T> {

    private final OpenCliResult raw;

    private final T structured;

    /**
     * @param raw        原始快照，不得为 null
     * @param structured 结构化对象，不得为 null
     * @param <T>        负载类型
     * @return 组合结果
     */
    public static <T> OpenCliTypedResult<T> of(OpenCliResult raw, T structured) {
        return new OpenCliTypedResult<>(raw, structured);
    }
}
