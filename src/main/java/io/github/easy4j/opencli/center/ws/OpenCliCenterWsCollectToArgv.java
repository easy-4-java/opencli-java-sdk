package io.github.easy4j.opencli.center.ws;

import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 将中心 {@code ws_agent_manager.dispatch_collect} 下发的 JSON 字段
 *（与 Python {@code agent_server.collect} 所用 {@code CollectRequest} 一致）
 * 转成本地 {@link io.github.easy4j.opencli.core.OpenCliExecutor#invoke(List)} 所需的 argv：
 * {@code [site, command, ...positional, --k v..., -f format]}。
 */
public final class OpenCliCenterWsCollectToArgv {

    private OpenCliCenterWsCollectToArgv() {
    }

    /**
     * @param site           {@code site} 字段
     * @param command        {@code command} 字段
     * @param positionalArgs {@code positional_args}，可为 null
     * @param args           {@code args} 映射，可为 null
     * @param format         {@code format}，默认 json
     * @return 不含可执行文件名的 token 列表
     */
    public static List<String> toArgv(
        String site,
        String command,
        List<String> positionalArgs,
        Map<String, Object> args,
        String format) {
        if (OpenCliStrings.isBlank(site) || OpenCliStrings.isBlank(command)) {
            throw new IllegalArgumentException("site and command must not be blank");
        }
        List<String> argv = new ArrayList<>();
        argv.add(site.trim());
        argv.add(command.trim());
        if (positionalArgs != null) {
            for (String p : positionalArgs) {
                if (p != null && !p.isEmpty()) {
                    argv.add(p);
                }
            }
        }
        if (args != null) {
            for (Map.Entry<String, Object> e : args.entrySet()) {
                if (e.getKey() == null || e.getKey().isEmpty()) {
                    continue;
                }
                argv.add("--" + e.getKey());
                if (e.getValue() != null) {
                    argv.add(Objects.toString(e.getValue()));
                }
            }
        }
        String fmt = OpenCliStrings.isBlank(format) ? "json" : format.trim();
        argv.add("-f");
        argv.add(fmt);
        return argv;
    }
}
