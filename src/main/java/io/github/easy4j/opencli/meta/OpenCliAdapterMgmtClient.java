package io.github.easy4j.opencli.meta;

import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli adapter *} 本地覆盖管理。
 */
@RequiredArgsConstructor
public final class OpenCliAdapterMgmtClient {

    private final OpenCliExecutor executor;

    private OpenCliResult invoke(List<String> rest) {
        List<String> tokens = new ArrayList<>();
        tokens.add("adapter");
        tokens.addAll(rest);
        return executor.invoke(tokens);
    }

    /** {@code adapter status} */
    public OpenCliResult status() {
        return invoke(OpenCliLists.of("status"));
    }

    /** {@code adapter eject <site>} */
    public OpenCliResult eject(String site) {
        return invoke(OpenCliLists.of("eject", site));
    }

    /** {@code adapter reset [site]} 或 {@code adapter reset --all} */
    public OpenCliResult reset(String site, boolean all) {
        List<String> args = new ArrayList<>();
        args.add("reset");
        if (all) {
            args.add("--all");
        } else if (site != null) {
            args.add(site);
        }
        return invoke(args);
    }

    /** @see #reset(String, boolean) */
    public OpenCliResult reset(String site) {
        return reset(site, false);
    }
}
