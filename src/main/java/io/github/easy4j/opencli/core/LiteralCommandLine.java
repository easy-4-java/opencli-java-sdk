package io.github.easy4j.opencli.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.exec.CommandLine;

/**
 * Internal literal argv bridge. Commons Exec's Argument constructor trims even
 * when handleQuoting is false, so raw values cannot be stored in its argument list.
 * Keep the original vector and supply it directly to the native launcher instead.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 */
final class LiteralCommandLine extends CommandLine {
    private final List<String> literalArguments = new ArrayList<>();

    LiteralCommandLine(String executable) {
        super(executable);
    }

    @Override
    public CommandLine addArgument(String argument, boolean handleQuoting) {
        if (handleQuoting) {
            throw new IllegalArgumentException("Literal argv does not support implicit quoting");
        }
        literalArguments.add(Objects.requireNonNull(argument, "argument"));
        return this;
    }

    @Override
    public String[] getArguments() {
        return literalArguments.toArray(new String[0]);
    }

    @Override
    public String[] toStrings() {
        String[] result = new String[literalArguments.size() + 1];
        result[0] = getExecutable();
        for (int i = 0; i < literalArguments.size(); i++) {
            result[i + 1] = literalArguments.get(i);
        }
        return result;
    }
}
