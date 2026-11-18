package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.parser.OpenCliHelpSchema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Local help queries verified against OpenCLI 1.8.8. Browser queries use a fixed help-only session token. */
public enum OpenCliHelpScope {
    ROOT_HELP(null, "opencli", null, null),
    BROWSER("opencli browser", null, null, "browser", "browser"),
    BROWSER_TAB("opencli browser <session> tab", null, null, "browser", "browser", "fixture", "tab"),
    BROWSER_GET("opencli browser <session> get", null, null, "browser", "browser", "fixture", "get"),
    BROWSER_STATE("opencli browser <session> state", "state", null, "browser", "browser", "fixture", "state"),
    META_AUTH("opencli auth", null, null, "auth", "auth"),
    META_LIST("opencli list", "list", null, null, "list"),
    SITE_NPM(null, null, "npm", null, "npm"),
    SITE_NPM_SEARCH("opencli npm search", "search", "npm", null, "npm", "search");

    private final String command;
    private final String name;
    private final String site;
    private final String namespace;
    private final List<String> arguments;

    OpenCliHelpScope(String command, String name, String site, String namespace, String... path) {
        this.command = command;
        this.name = name;
        this.site = site;
        this.namespace = namespace;
        List<String> argv = new ArrayList<>(Arrays.asList(path));
        argv.addAll(Arrays.asList("--help", "-f", "json"));
        this.arguments = Collections.unmodifiableList(argv);
    }

    List<String> arguments() {
        return arguments;
    }

    boolean matches(OpenCliHelpSchema schema) {
        return Objects.equals(command, schema.getCommand())
            && Objects.equals(name, schema.getName())
            && Objects.equals(site, schema.getSite())
            && Objects.equals(namespace, schema.getNamespace());
    }
}
