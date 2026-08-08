package io.github.easy4j.opencli.remote;

import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * 将 {@link io.github.easy4j.opencli.core.OpenCliExecutor#invoke(List)} 使用的 argv 列表
 *（{@code [adapter, command, ...]}） best-effort 映射为 Agent {@link OpenCliCollectRequest}。
 * 规则：前两个 token 为 {@code site}、{@code command}；其后从左到右扫描：
 * <ul>
 *   <li>{@code -f} / {@code --format} 及其下一 token 写入请求 {@code format}，不进入 args；</li>
 *   <li>{@code --key=value} 或 {@code --key} + 下一非选项值 → 进入 {@code args}（key 无 {@code --} 前缀）；</li>
 *   <li>单独 {@code --flag}（下一 token 以 {@code -} 开头或缺失）→ {@code args[flag]=true}；</li>
 *   <li>其余 token 按顺序进入 {@code positional_args}。</li>
 * </ul>
 * Agent 会自行追加 {@code -f &lt;format&gt;}，故本解析器会消费调用方已写的 {@code -f}，避免重复。
 */
@Slf4j/**

 * Best-effort mapping from the argv list used by
 * {@link io.github.easy4j.opencli.core.OpenCliExecutor#invoke(List)}
 * ({@code [adapter, command, ...]}) to an Agent {@link OpenCliCollectRequest}.
 *
 * <p>Parsing rules: the first two tokens are {@code site} and {@code command};
 * subsequent tokens are scanned left-to-right:</p>
 * <ul>
 *   <li>{@code -f} / {@code --format} and its next token are written to {@code format}, not args;</li>
 *   <li>{@code --key=value} or {@code --key} + next non-option value go to {@code args} (key without {@code --} prefix);</li>
 *   <li>Standalone {@code --flag} (next token starts with {@code -} or is absent) goes to {@code args[flag]=true};</li>
 *   <li>All other tokens go to {@code positional_args} in order.</li>
 * </ul>

 *

 * @author [@Loong Wan](https://github.com/loong10k)

 * @since 3.0.0

 */

public final class OpenCliArgvToCollectParser {

    private OpenCliArgvToCollectParser() {
    }

    /**
     * @param adapterAndRest 至少含 adapter、subcommand
     * @param defaultFormat  当 argv 未指定 {@code -f} 时使用
     * @param defaultMode    {@code bridge} 或 {@code cdp}
     * @param cdpEndpoint    可选，对应 collect body 的 {@code cdp_endpoint}
     * @return 非 null 请求对象
     */
    public static OpenCliCollectRequest parse(
        List<String> adapterAndRest,
        String defaultFormat,
        String defaultMode,
        String cdpEndpoint) {
        Objects.requireNonNull(adapterAndRest, "adapterAndRest");
        if (adapterAndRest.size() < 2) {
            throw new IllegalArgumentException(
                "remote invoke requires at least [adapter, command], got " + adapterAndRest.size() + " tokens");
        }
        String site = adapterAndRest.get(0).trim();
        String command = adapterAndRest.get(1).trim();
        if (OpenCliStrings.isBlank(site) || OpenCliStrings.isBlank(command)) {
            throw new IllegalArgumentException("adapter and command must not be blank");
        }

        String format = OpenCliStrings.isNotBlank(defaultFormat) ? defaultFormat.trim() : "json";
        String mode = OpenCliStrings.isNotBlank(defaultMode) ? defaultMode.trim() : "cdp";
        Map<String, Object> args = new LinkedHashMap<>();
        List<String> positional = new ArrayList<>();

        int i = 2;
        while (i < adapterAndRest.size()) {
            String raw = adapterAndRest.get(i);
            if (Objects.isNull(raw) || raw.isEmpty()) {
                log.warn("Skipping null or empty argv token at index={}", i);
                i++;
                continue;
            }
            String t = raw.trim();
            if (t.isEmpty()) {
                i++;
                continue;
            }

            if ("-f".equals(t) || "--format".equals(t)) {
                if (i + 1 >= adapterAndRest.size()) {
                    throw new IllegalArgumentException("-f requires a value");
                }
                format = Objects.requireNonNull(adapterAndRest.get(i + 1)).trim();
                i += 2;
                continue;
            }

            if (t.startsWith("--")) {
                if (t.contains("=")) {
                    int eq = t.indexOf('=');
                    String key = normalizeKey(t.substring(2, eq));
                    String val = t.substring(eq + 1);
                    if (isFormatKey(key)) {
                        format = val;
                    } else {
                        args.put(key, val);
                    }
                    i++;
                    continue;
                }
                String key = normalizeKey(t.substring(2));
                if (isFormatKey(key)) {
                    if (i + 1 >= adapterAndRest.size()) {
                        throw new IllegalArgumentException("--" + key + " requires a value");
                    }
                    format = adapterAndRest.get(i + 1).trim();
                    i += 2;
                    continue;
                }
                String next = i + 1 < adapterAndRest.size() ? adapterAndRest.get(i + 1) : null;
                if (Objects.nonNull(next) && !next.trim().startsWith("-")) {
                    args.put(key, next.trim());
                    i += 2;
                } else {
                    args.put(key, Boolean.TRUE);
                    i++;
                }
                continue;
            }

            positional.add(t);
            i++;
        }

        OpenCliCollectRequest.OpenCliCollectRequestBuilder b =
            OpenCliCollectRequest.builder().site(site).command(command).format(format).mode(mode);
        b.positionalArgs(positional);
        b.args(args);
        if (OpenCliStrings.isNotBlank(cdpEndpoint)) {
            b.cdpEndpoint(cdpEndpoint.trim());
        }
        OpenCliCollectRequest request = b.build();
        log.debug(
            "Parsed collect request site={} command={} format={} positionalCount={} argCount={}",
            site,
            command,
            format,
            positional.size(),
            args.size());
        return request;
    }

    private static String normalizeKey(String key) {
        if (Objects.isNull(key) || key.isEmpty()) {
            throw new IllegalArgumentException("empty option key");
        }
        return key.toLowerCase(Locale.ROOT);
    }

    private static boolean isFormatKey(String key) {
        return "format".equals(key) || "f".equals(key);
    }
}
