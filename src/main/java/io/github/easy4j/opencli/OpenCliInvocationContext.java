package io.github.easy4j.opencli;

import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

/** Immutable overrides for one schema-validated local invocation. */
@Getter
@Builder
public final class OpenCliInvocationContext {
    public enum WindowMode { FOREGROUND, BACKGROUND }
    public enum SiteSession { EPHEMERAL, PERSISTENT }
    public enum TraceMode { OFF, ON, RETAIN_ON_FAILURE }

    private final String profile;
    private final WindowMode window;
    private final SiteSession siteSession;
    private final Boolean keepTab;
    private final TraceMode trace;

    /**
     * @param defaults client defaults
     * @param invocation explicit invocation context
     * @param options explicit command flags
     * @return effective options, preserving the distinction between missing and false
     */
    public static Map<String, String> mergeOptions(OpenCliInvocationContext defaults, OpenCliInvocationContext invocation,
        Map<String, String> options) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<String, String> explicit = new LinkedHashMap<>();
        if (Objects.nonNull(defaults)) {
            result.putAll(defaults.flags());
        }
        if (Objects.nonNull(options)) {
            explicit.putAll(options);
        }
        if (Objects.nonNull(invocation)) {
            invocation.flags().forEach((flag, value) -> {
                if (explicit.containsKey(flag) && !Objects.equals(explicit.get(flag), value)) {
                    throw new IllegalArgumentException("Conflicting explicit invocation option " + flag);
                }
                explicit.put(flag, value);
            });
        }
        result.putAll(explicit);
        return Collections.unmodifiableMap(result);
    }

    private Map<String, String> flags() {
        Map<String, String> flags = new LinkedHashMap<>();
        if (Objects.nonNull(profile)) {
            if (OpenCliStrings.isBlank(profile) || profile.indexOf(0) >= 0) {
                throw new IllegalArgumentException("profile must be a nonblank literal identifier");
            }
            flags.put("--profile", profile);
        }
        putEnum(flags, "--window", window);
        putEnum(flags, "--site-session", siteSession);
        if (Objects.nonNull(keepTab)) {
            flags.put("--keep-tab", keepTab.toString());
        }
        putEnum(flags, "--trace", trace);
        return flags;
    }

    private static void putEnum(Map<String, String> flags, String name, Enum<?> value) {
        if (Objects.nonNull(value)) {
            flags.put(name, value.name().toLowerCase(Locale.ROOT).replace('_', '-'));
        }
    }
}
