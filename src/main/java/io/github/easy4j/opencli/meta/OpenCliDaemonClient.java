package io.github.easy4j.opencli.meta;

import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli daemon *} 守护进程管理。
 */
@RequiredArgsConstructor/**

 * Client for {@code opencli daemon *} daemon process management.

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public final class OpenCliDaemonClient {

    private final OpenCliExecutor executor;

    private OpenCliResult invoke(List<String> rest) {
        List<String> tokens = new ArrayList<>();
        tokens.add("daemon");
        tokens.addAll(rest);
        return executor.invoke(tokens);
    }

    public OpenCliResult status() {
        return invoke(OpenCliLists.of("status"));
    }

    public OpenCliResult stop() {
        return invoke(OpenCliLists.of("stop"));
    }

    public OpenCliResult restart() {
        return invoke(OpenCliLists.of("restart"));
    }
}
