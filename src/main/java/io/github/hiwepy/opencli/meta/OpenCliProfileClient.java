package io.github.hiwepy.opencli.meta;

import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli profile *} Browser Bridge Chrome profile 管理。
 */
@RequiredArgsConstructor
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
