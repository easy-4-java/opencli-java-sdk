package io.github.easy4j.opencli.meta;

import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli profile *} Browser Bridge Chrome profile 管理。
 */
@RequiredArgsConstructor/**

 * Client for {@code opencli profile *} Browser Bridge Chrome profile management.

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public final class OpenCliProfileClient {

    private final OpenCliExecutor executor;

    private OpenCliResult invoke(List<String> rest) {
        List<String> tokens = new ArrayList<>();
        tokens.add("profile");
        tokens.addAll(rest);
        return executor.invoke(tokens);
    }

    /** {@code profile list} */
    public OpenCliResult list() {
        return invoke(OpenCliLists.of("list"));
    }

    /** {@code profile rename <contextId> <alias>} */
    public OpenCliResult rename(String contextId, String alias) {
        return invoke(OpenCliLists.of("rename", contextId, alias));
    }

    /** {@code profile use <profile>} */
    public OpenCliResult use(String profileAliasOrContextId) {
        return invoke(OpenCliLists.of("use", profileAliasOrContextId));
    }
}
