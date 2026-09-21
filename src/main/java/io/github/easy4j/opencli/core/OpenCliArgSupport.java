package io.github.easy4j.opencli.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * CLI argument assembly utilities: merges business segments with pass-through
 * {@code additionalRawArgs} without changing argument values.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
public final class OpenCliArgSupport {

    private OpenCliArgSupport() {
    }

    /**
     * Capture values before execution or queuing. Validate indices without exposing other values.
     */
    static List<String> snapshotValues(List<String> values, String field) {
        Objects.requireNonNull(values, field);
        List<String> copy = new ArrayList<>(values);
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i) == null) {
                throw new IllegalArgumentException(field + "[" + i + "] must not be null");
            }
        }
        return Collections.unmodifiableList(copy);
    }

    /**
     * Merge optional segments into a new argv list. Null segments are absent;
     * null elements are invalid, while empty and whitespace-only values are preserved.
     *
     * @param prefix command and modeled arguments, or null
     * @param additionalRawArgs extra literal arguments, or null
     * @return a new list, without modifying either source
     */
    public static List<String> merge(List<String> prefix, List<String> additionalRawArgs) {
        List<String> out = new ArrayList<>();
        if (prefix != null) {
            out.addAll(snapshotValues(prefix, "prefix"));
        }
        if (additionalRawArgs != null) {
            out.addAll(snapshotValues(additionalRawArgs, "additionalRawArgs"));
        }
        return out;
    }

    /**
     * Append one literal {@code --name=value} token.
     *
     * @param target destination list
     * @param name full option name, including {@code --}
     * @param value non-null value, which may be empty
     */
    public static void addOptionEquals(List<String> target, String name, String value) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (!name.startsWith("--")) {
            throw new IllegalArgumentException("name must start with '--', got: " + name);
        }
        String key = name.endsWith("=") ? name.substring(0, name.length() - 1) : name;
        target.add(key + "=" + value);
    }

    /**
     * Append a {@code --flag value} pair without altering the value.
     *
     * @param target destination list
     * @param flag option name
     * @param value non-null value, which may be empty
     */
    public static void addOptionPair(List<String> target, String flag, String value) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(flag, "flag");
        Objects.requireNonNull(value, "value");
        target.add(flag);
        target.add(value);
    }

    /**
     * Append a pair when the optional value is non-null.
     *
     * @param target destination list
     * @param flag option name
     * @param value optional value
     */
    public static void addOptionPairIfPresent(List<String> target, String flag, Object value) {
        if (Objects.nonNull(value)) {
            addOptionPair(target, flag, String.valueOf(value));
        }
    }

    /**
     * Append a presence-only flag when enabled. This helper is not a valued boolean option.
     *
     * @param target destination list
     * @param flag option name
     * @param enabled true to append; null/false to omit
     */
    public static void addFlagIfTrue(List<String> target, String flag, Boolean enabled) {
        if (Boolean.TRUE.equals(enabled)) {
            Objects.requireNonNull(target, "target").add(flag);
        }
    }
}
