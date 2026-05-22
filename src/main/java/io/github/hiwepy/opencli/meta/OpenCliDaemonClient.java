package io.github.hiwepy.opencli.meta;

import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.util.OpenCliLists;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * {@code opencli daemon *} 守护进程管理。
 */
@RequiredArgsConstructor
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
