package io.github.easy4j.opencli.remote;

import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/** Maps the supported adapter argv grammar to the legacy HTTP collect DTO. */
@Slf4j
public final class OpenCliArgvToCollectParser {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*");

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
        String site = requireIdentifier(adapterAndRest.get(0));
        String command = requireIdentifier(adapterAndRest.get(1));

        String format = OpenCliStrings.isNotBlank(defaultFormat) ? defaultFormat.trim() : "json";
        String mode = OpenCliStrings.isNotBlank(defaultMode) ? defaultMode.trim() : "cdp";
        Map<String, Object> args = new LinkedHashMap<>();
        List<String> positional = new ArrayList<>();
        boolean formatSpecified = false;

        int index = 2;
        while (index < adapterAndRest.size()) {
            String token = adapterAndRest.get(index++);
            if (Objects.isNull(token) || token.isEmpty() || !token.equals(token.trim())) {
                throw new IllegalArgumentException("Legacy collect cannot preserve an empty or padded token");
            }
            if (!token.startsWith("-")) {
                if (formatSpecified || !args.isEmpty()) {
                    throw new IllegalArgumentException("Legacy collect requires positional arguments before options");
                }
                positional.add(token);
                continue;
            }

            int separator = token.indexOf('=');
            String option = separator < 0 ? token : token.substring(0, separator);
            boolean shortFormat = "-f".equals(option) && separator < 0;
            if (!shortFormat && !option.startsWith("--")) {
                throw new IllegalArgumentException("Legacy collect does not support this option syntax");
            }
            String key = shortFormat ? "format" : requireIdentifier(option.substring(2));
            boolean isFormat = "format".equals(key) || "f".equals(key);
            String value;
            if (separator >= 0) {
                value = requireValue(token.substring(separator + 1));
            } else {
                if (index == adapterAndRest.size()) {
                    throw new IllegalArgumentException("Legacy collect requires an explicit option value");
                }
                value = requireValue(adapterAndRest.get(index++));
            }
            if (isFormat) {
                if (formatSpecified) {
                    throw new IllegalArgumentException("Legacy collect requires a single explicit format value");
                }
                format = value;
                formatSpecified = true;
            } else if (Objects.nonNull(args.putIfAbsent(key, value))) {
                throw new IllegalArgumentException("Legacy collect cannot preserve a repeated option");
            }
        }

        OpenCliCollectRequest.OpenCliCollectRequestBuilder b =
            OpenCliCollectRequest.builder().site(site).command(command).format(format).mode(mode);
        b.positionalArgs(positional);
        b.args(args);
        if (OpenCliStrings.isNotBlank(cdpEndpoint)) {
            b.cdpEndpoint(cdpEndpoint.trim());
        }
        OpenCliCollectRequest request = b.build();
        log.debug("Parsed collect request positionalCount={} argCount={}", positional.size(), args.size());
        return request;
    }

    private static String requireIdentifier(String value) {
        if (Objects.isNull(value) || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Legacy collect requires a valid adapter, command or option identifier");
        }
        return value;
    }

    private static String requireValue(String value) {
        if (Objects.isNull(value) || value.isEmpty() || !value.equals(value.trim()) || value.startsWith("-")) {
            throw new IllegalArgumentException("Legacy collect cannot preserve this option value");
        }
        return value;
    }
}
