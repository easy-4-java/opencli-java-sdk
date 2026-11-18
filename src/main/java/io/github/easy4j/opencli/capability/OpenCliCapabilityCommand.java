package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.parser.OpenCliHelpSchema;
import io.github.easy4j.opencli.parser.OpenCliHelpSchema.Argument;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

/** Converts one explicitly selected help leaf into validated literal CLI arguments. */
public final class OpenCliCapabilityCommand {
    private static final Pattern COMMAND_TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*");
    private static final Pattern OPTION_TOKEN = Pattern.compile("--[A-Za-z0-9][A-Za-z0-9-]*|-[A-Za-z0-9]");

    private OpenCliCapabilityCommand() {
    }

    /**
     * @param snapshot currently available structured help
     * @param command exact leaf display path from help, including any placeholders
     * @param session explicit browser session, or null for a non-browser leaf
     * @param positionals literal values in declared order, or null
     * @param options declared option spellings and values, with null values for bare flags
     * @return immutable literal arguments excluding the executable
     */
    public static List<String> bind(OpenCliCapabilitySnapshot snapshot, String command, String session,
        List<String> positionals, Map<String, String> options) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(command, "command");
        if (snapshot.getStatus() != OpenCliCapabilitySnapshot.Status.AVAILABLE
            || Objects.isNull(snapshot.getHelp()) || Objects.isNull(snapshot.getHelp().getSchema())) {
            throw new IllegalStateException("Current structured help is unavailable");
        }
        OpenCliHelpSchema scope = snapshot.getHelp().getSchema();
        OpenCliHelpSchema leaf = locateLeaf(scope, command);
        String[] path = command.split(" ", -1);
        if (path.length < 3 || !"opencli".equals(path[0])) {
            throw new IllegalArgumentException("The help command path cannot be bound");
        }

        List<String> prefixOptions = new ArrayList<>();
        List<String> leafOptions = new ArrayList<>();
        appendOptions(scope, leaf, options, prefixOptions, leafOptions);

        List<String> argv = new ArrayList<>();
        argv.add(path[1]);
        argv.addAll(prefixOptions);
        boolean sessionUsed = false;
        for (int index = 2; index < path.length; index++) {
            String token = path[index];
            if ("<session>".equals(token) && index == 2 && "browser".equals(path[1])) {
                if (OpenCliStrings.isBlank(session) || !COMMAND_TOKEN.matcher(session).matches()) {
                    throw new IllegalArgumentException("A literal browser session identifier is required");
                }
                argv.add(session);
                sessionUsed = true;
            } else {
                if (!COMMAND_TOKEN.matcher(token).matches()) {
                    throw new IllegalArgumentException("The help command path contains an unsupported token");
                }
                argv.add(token);
            }
        }
        if (!sessionUsed && Objects.nonNull(session)) {
            throw new IllegalArgumentException("This command has no browser session binding");
        }
        argv.addAll(leafOptions);
        List<String> values = OpenCliArgSupport.merge(null, positionals);
        validatePositionals(leaf.getPositionals(), values);
        if (!values.isEmpty()) {
            argv.add("--");
            argv.addAll(values);
        }
        return Collections.unmodifiableList(argv);
    }

    private static OpenCliHelpSchema locateLeaf(OpenCliHelpSchema scope, String command) {
        if (command.equals(scope.getCommand()) && scope.getCommands().isEmpty()) {
            return scope;
        }
        return scope.getCommands().stream()
            .filter(child -> command.equals(child.getCommand()) && child.getCommands().isEmpty())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("The requested command is not a leaf in this help scope"));
    }

    private static void validatePositionals(List<Argument> definitions, List<String> values) {
        int consumed = 0;
        for (Argument definition : definitions) {
            int count = Boolean.TRUE.equals(definition.getVariadic()) ? values.size() - consumed
                : Math.min(1, values.size() - consumed);
            if (count == 0 && Boolean.TRUE.equals(definition.getRequired())) {
                throw invalid(definition, "is required");
            }
            for (int index = 0; index < count; index++) {
                validateValue(definition, values.get(consumed++));
            }
        }
        if (consumed != values.size()) {
            throw new IllegalArgumentException("Too many positional arguments");
        }
    }

    private static void appendOptions(OpenCliHelpSchema scope, OpenCliHelpSchema leaf, Map<String, String> options,
        List<String> prefix, List<String> local) {
        Map<String, Option> definitions = new LinkedHashMap<>();
        index(definitions, inherited(leaf.getGlobalOptions(), scope.getGlobalOptions()), prefix);
        index(definitions, inherited(leaf.getNamespaceOptions(), scope.getNamespaceOptions()), prefix);
        index(definitions, inherited(leaf.getCommonOptions(), scope.getCommonOptions()), local);
        index(definitions, leaf.getCommandOptions(), local);
        Set<Option> supplied = new HashSet<>();
        if (Objects.nonNull(options)) {
            new LinkedHashMap<>(options).forEach((flag, value) -> {
                Option option = definitions.get(flag);
                if (Objects.isNull(option)) {
                    throw new IllegalArgumentException("An option identifier was not declared by help");
                }
                if (!supplied.add(option)) {
                    throw invalid(option.argument, "was supplied more than once");
                }
                option.append(flag, value);
            });
        }
        for (Option option : new HashSet<>(definitions.values())) {
            if (Boolean.TRUE.equals(option.argument.getRequired()) && !supplied.contains(option)) {
                throw invalid(option.argument, "is required");
            }
        }
    }

    private static List<Argument> inherited(List<Argument> own, List<Argument> parent) {
        return own.isEmpty() ? parent : own;
    }

    private static void index(Map<String, Option> definitions, List<Argument> arguments, List<String> target) {
        for (Argument argument : arguments) {
            Option option = new Option(argument, target);
            String flags = argument.getFlags();
            if (OpenCliStrings.isBlank(flags)) {
                flags = "--" + argument.getName();
            }
            for (String flag : flags.split("[,\\s]+")) {
                if (OPTION_TOKEN.matcher(flag).matches() && Objects.nonNull(definitions.putIfAbsent(flag, option))) {
                    throw invalid(argument, "has an ambiguous flag in this help scope");
                }
            }
        }
    }

    private static void validateValue(Argument argument, String value) {
        if (value.indexOf(0) >= 0) {
            throw invalid(argument, "contains an invalid character");
        }
        if (!argument.getChoices().isEmpty() && !argument.getChoices().contains(value)) {
            throw invalid(argument, "must be one of the declared choices");
        }
    }

    private static IllegalArgumentException invalid(Argument argument, String reason) {
        return new IllegalArgumentException("Argument '" + argument.getName() + "' " + reason);
    }

    @RequiredArgsConstructor
    private static class Option {
        private final Argument argument;
        private final List<String> target;

        private void append(String flag, String value) {
            String arity = argument.getTakesValue();
            String flags = argument.getFlags();
            if (Objects.isNull(arity)) {
                if (Boolean.TRUE.equals(argument.getValueRequired()) || Boolean.TRUE.equals(argument.getRequired())
                    || (Objects.nonNull(flags) && flags.indexOf('<') >= 0)) {
                    arity = "required";
                } else if (OpenCliStrings.isBlank(flags) || flags.indexOf('[') >= 0) {
                    arity = "optional";
                } else {
                    arity = "none";
                }
            }
            if (flag.startsWith("--no-") || Boolean.TRUE.equals(argument.getNegate())) {
                arity = "none";
            }
            switch (arity) {
                case "required":
                    if (Objects.isNull(value)) {
                        throw invalid(argument, "requires a value");
                    }
                    validateValue(argument, value);
                    OpenCliArgSupport.addOptionPair(target, flag, value);
                    break;
                case "optional":
                    if (Objects.isNull(value)) {
                        target.add(flag);
                    } else {
                        validateValue(argument, value);
                        if (flag.startsWith("--")) {
                            OpenCliArgSupport.addOptionEquals(target, flag, value);
                        } else if (!value.isEmpty()) {
                            target.add(flag + value);
                        } else {
                            throw invalid(argument, "requires a long option spelling for an empty value");
                        }
                    }
                    break;
                case "none":
                    if (Objects.nonNull(value)) {
                        throw invalid(argument, "does not take a value");
                    }
                    target.add(flag);
                    break;
                default:
                    throw invalid(argument, "has unknown value semantics");
            }
        }
    }
}
