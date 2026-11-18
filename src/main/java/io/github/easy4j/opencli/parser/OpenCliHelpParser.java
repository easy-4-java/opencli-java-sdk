package io.github.easy4j.opencli.parser;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import io.github.easy4j.opencli.parser.OpenCliHelpResult.Reason;
import io.github.easy4j.opencli.parser.OpenCliHelpResult.Status;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Decodes the OpenCLI structured-help response shapes without executing their contents. */
public final class OpenCliHelpParser {

    private static final ObjectReader READER = new JsonMapper().reader().with(
        DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
        DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
        DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private OpenCliHelpParser() {
    }

    /**
     * @param rawHelp stdout from a bounded help probe or a previously collected sample
     * @return a structured schema or a reason it could not be recognized
     */
    public static OpenCliHelpResult parse(String rawHelp) {
        if (Objects.isNull(rawHelp)) {
            return unknown(rawHelp, Reason.EMPTY);
        }
        int start = 0;
        while (start < rawHelp.length() && " \t\r\n".indexOf(rawHelp.charAt(start)) >= 0) {
            start++;
        }
        if (start == rawHelp.length()) {
            return unknown(rawHelp, Reason.EMPTY);
        }
        char first = rawHelp.charAt(start);
        if (first < ' ') {
            return unknown(rawHelp, Reason.INVALID_JSON);
        }
        if (first != '{') {
            return unknown(rawHelp, Reason.UNSUPPORTED_FORMAT);
        }

        JsonNode root;
        try {
            root = READER.readTree(rawHelp);
        } catch (JacksonException ex) {
            return unknown(rawHelp, Reason.INVALID_JSON);
        }
        try {
            if (!isHelpRoot(root)) {
                return unknown(rawHelp, Reason.INVALID_SCHEMA);
            }
            return new OpenCliHelpResult(Status.AVAILABLE, null, rawHelp, schema(root));
        } catch (IllegalArgumentException ex) {
            return unknown(rawHelp, Reason.INVALID_SCHEMA);
        }
    }

    private static OpenCliHelpResult unknown(String rawHelp, Reason reason) {
        return new OpenCliHelpResult(Status.UNKNOWN, reason, rawHelp, null);
    }

    private static boolean isHelpRoot(JsonNode root) {
        if (!root.isObject()) {
            return false;
        }
        boolean directory = root.path("commands").isArray();
        if (directory && "opencli".equals(text(root.path("name"))) && root.has("site_adapters")) {
            JsonNode sites = root.path("site_adapters").path("sites");
            if (!sites.isArray()) {
                return false;
            }
            for (JsonNode site : sites) {
                requiredName(site);
            }
            return true;
        }
        if (directory && OpenCliStrings.isNotBlank(text(root.path("site")))) {
            return true;
        }
        return (OpenCliStrings.isNotBlank(text(root.path("namespace")))
                || OpenCliStrings.isNotBlank(text(root.path("site"))))
            && OpenCliStrings.isNotBlank(text(root.path("command")))
            && (directory || (OpenCliStrings.isNotBlank(text(root.path("name")))
                && (root.path("positionals").isArray() || root.path("command_options").isArray())));
    }

    private static OpenCliHelpSchema schema(JsonNode node) {
        String name = text(node.path("name"));
        String command = text(node.path("command"));
        String site = text(node.path("site"));
        if (!node.isObject() || (OpenCliStrings.isBlank(name)
                && OpenCliStrings.isBlank(command) && OpenCliStrings.isBlank(site))) {
            throw invalidSchema();
        }
        return OpenCliHelpSchema.builder()
            .name(name)
            .command(command)
            .namespace(text(node.path("namespace")))
            .site(site)
            .aliases(array(node.path("aliases"), OpenCliHelpParser::requiredName))
            .commands(array(node.path("commands"), OpenCliHelpParser::schema))
            .positionals(array(node.path("positionals"), OpenCliHelpParser::argument))
            .commandOptions(array(node.path("command_options"), OpenCliHelpParser::argument))
            .namespaceOptions(array(node.path("namespace_options"), OpenCliHelpParser::argument))
            .globalOptions(array(node.path("global_options"), OpenCliHelpParser::argument))
            .commonOptions(array(node.path("common_options"), OpenCliHelpParser::argument))
            .sourceJson(node.toString())
            .build();
    }

    private static OpenCliHelpSchema.Argument argument(JsonNode node) {
        if (!node.isObject()) {
            throw invalidSchema();
        }
        JsonNode defaultValue = node.path("default");
        return OpenCliHelpSchema.Argument.builder()
            .name(requiredName(node.path("name")))
            .flags(text(node.path("flags")))
            .type(text(node.path("type")))
            .takesValue(text(node.path("takes_value")))
            .required(bool(node.path("required")))
            .negate(bool(node.path("negate")))
            .variadic(bool(node.path("variadic")))
            .choices(array(node.path("choices"), OpenCliHelpParser::requiredText))
            .defaultJson(defaultValue.isMissingNode() ? null : defaultValue.toString())
            .sourceJson(node.toString())
            .build();
    }

    private static <T> List<T> array(JsonNode node, Function<JsonNode, T> decode) {
        if (node.isMissingNode()) {
            return Collections.emptyList();
        }
        if (!node.isArray()) {
            throw invalidSchema();
        }
        List<T> values = new ArrayList<>(node.size());
        for (JsonNode value : node) {
            values.add(decode.apply(value));
        }
        return Collections.unmodifiableList(values);
    }

    private static String text(JsonNode node) {
        if (node.isMissingNode()) {
            return null;
        }
        if (!node.isTextual()) {
            throw invalidSchema();
        }
        return node.asText();
    }

    private static String requiredText(JsonNode node) {
        String value = text(node);
        if (Objects.isNull(value)) {
            throw invalidSchema();
        }
        return value;
    }

    private static String requiredName(JsonNode node) {
        String value = text(node);
        if (OpenCliStrings.isBlank(value)) {
            throw invalidSchema();
        }
        return value;
    }

    private static Boolean bool(JsonNode node) {
        if (node.isMissingNode()) {
            return null;
        }
        if (!node.isBoolean()) {
            throw invalidSchema();
        }
        return node.booleanValue();
    }

    private static IllegalArgumentException invalidSchema() {
        return new IllegalArgumentException("Invalid OpenCLI help schema");
    }
}
