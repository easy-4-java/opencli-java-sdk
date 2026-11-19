package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.util.OpenCliStrings;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Structured adapter request with immutable literal values and ordered options.
 * The legacy options Map retains Boolean presence-only semantics; use
 * {@link OpenCliOption} for repeated values, valued false and explicit negation.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
@Getter
@Builder
public final class OpenCliAdapterCommandRequest {
    private final String subcommand;

    @Getter(AccessLevel.NONE)
    @Singular("positional")
    private final List<String> positionals;

    @Getter(AccessLevel.NONE)
    private final Map<String, Object> options;

    @Getter(AccessLevel.NONE)
    @Singular("option")
    private final List<OpenCliOption> orderedOptions;

    /**
     * Builder customisation snapshots legacy option values before they can be
     * changed through the caller's Map or a mutable value object.
     */
    public static class OpenCliAdapterCommandRequestBuilder {
        private Map<String, Object> options;

        /** @param source legacy single-value options @return this builder */
        public OpenCliAdapterCommandRequestBuilder options(Map<String, Object> source) {
            options = snapshotOptions(source);
            return this;
        }

        @Override
        public String toString() {
            return "OpenCliAdapterCommandRequestBuilder";
        }
    }

    /** @return an immutable copy of positional values */
    public List<String> getPositionals() {
        return positionals == null ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(positionals));
    }

    /** @return an immutable snapshot of legacy options */
    public Map<String, Object> getOptions() {
        return options == null ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }

    /** @return immutable, ordered option occurrences */
    public List<OpenCliOption> getOrderedOptions() {
        return orderedOptions == null ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(orderedOptions));
    }

    /** @return the validated subcommand and complete literal argv */
    public List<String> toSubcommandAndArgs() {
        Objects.requireNonNull(subcommand, "subcommand");
        if (OpenCliStrings.isBlank(subcommand)) {
            throw new IllegalArgumentException("subcommand must not be blank");
        }
        List<String> tokens = new ArrayList<>();
        tokens.add(subcommand.trim());
        if (positionals != null) {
            tokens.addAll(OpenCliArgSupport.snapshotValues(positionals, "positionals"));
        }
        Set<String> legacyFlags = new HashSet<>();
        if (options != null) {
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                String flag = appendLegacyOption(tokens, entry.getKey(), entry.getValue());
                if (flag != null && !legacyFlags.add(flag)) {
                    throw new IllegalArgumentException("Ambiguous duplicate legacy option identifier");
                }
            }
        }
        Map<String, OpenCliOptionSchema> seen = new HashMap<>();
        Map<String, String> wireNames = new HashMap<>();
        if (orderedOptions != null) {
            for (int i = 0; i < orderedOptions.size(); i++) {
                OpenCliOption occurrence = orderedOptions.get(i);
                if (occurrence == null) {
                    throw new IllegalArgumentException("orderedOptions[" + i + "] must not be null");
                }
                OpenCliOptionSchema schema = occurrence.getSchema();
                String name = schema.getName();
                OpenCliOptionSchema previous = seen.put(name, schema);
                if (Objects.nonNull(previous) && (!schema.isRepeatable() || !previous.equals(schema))) {
                    throw new IllegalArgumentException("Repeated option is not allowed by one consistent schema");
                }
                reserveOptionName(wireNames, legacyFlags, name, name);
                if (schema.getKind() == OpenCliOptionSchema.Kind.NEGATABLE_FLAG) {
                    reserveOptionName(wireNames, legacyFlags, schema.negatedName(), name);
                }
                tokens.addAll(occurrence.toTokens());
            }
        }
        return tokens;
    }

    private static void reserveOptionName(Map<String, String> wireNames, Set<String> legacyFlags,
        String wireName, String canonicalName) {
        if (legacyFlags.contains(wireName)) {
            throw new IllegalArgumentException("Legacy and ordered options must not overlap");
        }
        String previous = wireNames.putIfAbsent(wireName, canonicalName);
        if (Objects.nonNull(previous) && !previous.equals(canonicalName)) {
            throw new IllegalArgumentException("Different schemas reserve the same option identifier");
        }
    }

    private static Map<String, Object> snapshotOptions(Map<String, Object> source) {
        if (source == null) { return Collections.emptyMap(); }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : new LinkedHashMap<>(source).entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                Class<?> type = value.getClass();
                if (type != String.class && type != Boolean.class && type != Byte.class
                    && type != Short.class && type != Integer.class && type != Long.class
                    && type != Float.class && type != Double.class && type != BigInteger.class
                    && type != BigDecimal.class) {
                    value = OpenCliArgSupport.stringValue(value, "option value");
                }
            }
            snapshot.put(entry.getKey(), value);
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private static String appendLegacyOption(List<String> target, String name, Object value) {
        if (OpenCliStrings.isBlank(name) || value == null || Boolean.FALSE.equals(value)) {
            return null;
        }
        String normalized = name.trim();
        String flag = normalized.startsWith("-") ? normalized : "--" + normalized;
        target.add(flag);
        if (!(value instanceof Boolean)) {
            target.add(OpenCliArgSupport.stringValue(value, "option value"));
        }
        return flag;
    }

    /**
     * @param subcommand command identifier
     * @param positionals optional positional values
     * @param options optional legacy single-value options
     * @return a structured request
     */
    public static OpenCliAdapterCommandRequest of(
        String subcommand, List<String> positionals, Map<String, Object> options) {
        OpenCliAdapterCommandRequestBuilder b = builder().subcommand(subcommand);
        if (positionals != null) { b.positionals(positionals); }
        if (options != null) { b.options(options); }
        return b.build();
    }
}
