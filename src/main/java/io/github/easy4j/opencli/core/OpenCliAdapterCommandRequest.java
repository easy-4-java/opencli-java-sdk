package io.github.easy4j.opencli.core;

import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Structured adapter request. Positional and valued-option contents are literal;
 * command and option identifiers are validated separately.
 * The legacy options map represents a single-valued subset: Boolean values
 * retain their historical presence-only flag semantics.
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
    @Builder.Default
    private final Map<String, Object> options = Collections.emptyMap();

    /** @return an immutable copy of positional values */
    public List<String> getPositionals() {
        if (Objects.isNull(positionals)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(positionals));
    }

    /** @return an immutable copy of named options */
    public Map<String, Object> getOptions() {
        if (Objects.isNull(options)) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }

    /**
     * @return subcommand, then unchanged positional values and named options
     */
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
        if (Objects.nonNull(options)) {
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                appendOption(tokens, entry.getKey(), entry.getValue());
            }
        }
        return tokens;
    }

    private static void appendOption(List<String> target, String name, Object value) {
        if (OpenCliStrings.isBlank(name) || Objects.isNull(value)) {
            return;
        }
        String flag = name.startsWith("-") ? name.trim() : "--" + name.trim();
        if (value instanceof Boolean) {
            if (((Boolean) value).booleanValue()) {
                target.add(flag);
            }
            return;
        }
        target.add(flag);
        target.add(String.valueOf(value));
    }

    /**
     * @param subcommand command identifier
     * @param positionals optional positional values
     * @param options optional legacy single-value options
     * @return a structured request
     */
    public static OpenCliAdapterCommandRequest of(
        String subcommand,
        List<String> positionals,
        Map<String, Object> options) {
        OpenCliAdapterCommandRequestBuilder b = builder().subcommand(subcommand);
        if (Objects.nonNull(positionals)) {
            b.positionals(positionals);
        }
        if (Objects.nonNull(options) && !options.isEmpty()) {
            b.options(new LinkedHashMap<>(options));
        }
        return b.build();
    }
}
