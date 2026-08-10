package io.github.easy4j.opencli.meta;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 * {@link OpenCliExternalClient#passthrough(OpenCliExternalPassthroughOptions)} 的参数对象。
 * <p>替代测试中向 {@code passthrough(name, List.of(...))} 传入手工 argv 列表。</p>
 */
@Data
@Builder/**

 * Options for {@link OpenCliExternalClient#passthrough(OpenCliExternalPassthroughOptions)}.
 *
 * <p>Replaces manual argv list passing in tests via {@code passthrough(name, List.of(...))}.</p>

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public class OpenCliExternalPassthroughOptions {

    /** 已注册的外部 CLI 名（根级命令）。 */
    private String externalCliName;

    /** 传给外部二进制的参数片段。 */
    @Singular("arg")
    private List<String> args;
}
