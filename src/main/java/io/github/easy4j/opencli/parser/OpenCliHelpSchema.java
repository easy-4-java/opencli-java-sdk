package io.github.easy4j.opencli.parser;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/** Read-only projection of a recognized help document or command entry. */
@Getter
@Builder(access = AccessLevel.PACKAGE)
public final class OpenCliHelpSchema {

    private final String name;
    /** Upstream display path, including any unbound placeholders. */
    private final String command;
    private final String namespace;
    private final String site;
    private final Boolean browser;
    private final List<String> aliases;
    private final List<OpenCliHelpSchema> commands;
    private final List<Argument> positionals;
    private final List<Argument> commandOptions;
    private final List<Argument> namespaceOptions;
    private final List<Argument> globalOptions;
    private final List<Argument> commonOptions;
    private final List<Argument> browserCommonOptions;
    /** Source JSON for this entry, including fields the SDK does not interpret. */
    private final String sourceJson;

    /** Option or positional metadata as reported by OpenCLI. Missing scalar fields are null. */
    @Getter
    @Builder(access = AccessLevel.PACKAGE)
    public static final class Argument {

        private final String name;
        private final String flags;
        private final String type;
        private final String takesValue;
        private final Boolean required;
        private final Boolean valueRequired;
        private final Boolean negate;
        private final Boolean variadic;
        private final List<String> choices;
        /** JSON representation of the reported default. Explicit null is the literal "null". */
        private final String defaultJson;
        private final String sourceJson;
    }
}
