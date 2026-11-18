package io.github.easy4j.opencli.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Explicit option input semantics. Definitions come from a known command contract,
 * not guesses about a raw argument vector. This is not an output schema.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
public final class OpenCliOptionSchema {
    /** Input arity and negation semantics. */
    public enum Kind { FLAG, VALUE, NEGATABLE_FLAG }

    private static final Pattern NAME = Pattern.compile("(?:--[A-Za-z0-9][A-Za-z0-9-]*|-[A-Za-z0-9])");
    private final String name;
    private final Kind kind;
    private final boolean repeatable;

    private OpenCliOptionSchema(String name, Kind kind, boolean repeatable) {
        Objects.requireNonNull(name, "name");
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Option schema requires a valid flag identifier");
        }
        if (kind == Kind.NEGATABLE_FLAG && (!name.startsWith("--") || name.startsWith("--no-"))) {
            throw new IllegalArgumentException("Negatable schema requires a positive long flag identifier");
        }
        this.name = name;
        this.kind = kind;
        this.repeatable = repeatable;
    }

    /** @param name flag identifier @return a presence-only, nonrepeatable flag */
    public static OpenCliOptionSchema flag(String name) {
        return new OpenCliOptionSchema(name, Kind.FLAG, false);
    }

    /**
     * @param name option identifier
     * @param repeatable whether repeated occurrences are accepted by the command
     * @return an option taking one literal value per occurrence
     */
    public static OpenCliOptionSchema value(String name, boolean repeatable) {
        return new OpenCliOptionSchema(name, Kind.VALUE, repeatable);
    }

    /** @param name positive long flag identifier @return a flag supporting explicit negation */
    public static OpenCliOptionSchema negatableFlag(String name) {
        return new OpenCliOptionSchema(name, Kind.NEGATABLE_FLAG, false);
    }

    /** @return canonical flag identifier */
    public String getName() { return name; }

    /** @return declared input kind */
    public Kind getKind() { return kind; }

    /** @return whether the command accepts repeated occurrences */
    public boolean isRepeatable() { return repeatable; }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (!(other instanceof OpenCliOptionSchema)) { return false; }
        OpenCliOptionSchema that = (OpenCliOptionSchema) other;
        return name.equals(that.name) && kind == that.kind && repeatable == that.repeatable;
    }

    @Override
    public int hashCode() { return Objects.hash(name, kind, repeatable); }
}
