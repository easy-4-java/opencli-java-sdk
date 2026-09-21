package io.github.easy4j.opencli.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One immutable, schema-checked option occurrence. Preserve occurrence ordering
 * by adding these to a request builder rather than using a Map for repeated flags.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
public final class OpenCliOption {
    private final OpenCliOptionSchema schema;
    private final String value;
    private final boolean negated;

    private OpenCliOption(OpenCliOptionSchema schema, String value, boolean negated) {
        this.schema = schema;
        this.value = value;
        this.negated = negated;
    }

    /**
     * @param schema a valued-option definition
     * @param value non-null value; false is the literal value "false", not absence
     * @return an occurrence capturing the value immediately
     */
    public static OpenCliOption value(OpenCliOptionSchema schema, Object value) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(value, "value");
        if (schema.getKind() != OpenCliOptionSchema.Kind.VALUE) {
            throw new IllegalArgumentException("A flag schema cannot consume a value");
        }
        return new OpenCliOption(schema, String.valueOf(value), false);
    }

    /** @param schema a flag definition @return explicit positive presence */
    public static OpenCliOption present(OpenCliOptionSchema schema) {
        Objects.requireNonNull(schema, "schema");
        if (schema.getKind() == OpenCliOptionSchema.Kind.VALUE) {
            throw new IllegalArgumentException("A valued option requires a value");
        }
        return new OpenCliOption(schema, null, false);
    }

    /** @param schema a negatable flag definition @return an explicit negative flag */
    public static OpenCliOption negated(OpenCliOptionSchema schema) {
        Objects.requireNonNull(schema, "schema");
        if (schema.getKind() != OpenCliOptionSchema.Kind.NEGATABLE_FLAG) {
            throw new IllegalArgumentException("This option schema does not declare negation");
        }
        return new OpenCliOption(schema, null, true);
    }

    /** @return immutable input definition */
    public OpenCliOptionSchema getSchema() { return schema; }

    /** @return the captured value, or null for a flag */
    public String getValue() { return value; }

    /** @return whether this is explicit negative presence */
    public boolean isNegated() { return negated; }

    /** @return immutable literal tokens; no quoting or trimming is applied */
    public List<String> toTokens() {
        String flag = negated ? "--no-" + schema.getName().substring(2) : schema.getName();
        return value == null ? Collections.singletonList(flag)
            : Collections.unmodifiableList(Arrays.asList(flag, value));
    }
}
