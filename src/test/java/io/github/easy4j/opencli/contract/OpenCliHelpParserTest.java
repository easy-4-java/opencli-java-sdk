package io.github.easy4j.opencli.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.easy4j.opencli.parser.OpenCliHelpParser;
import io.github.easy4j.opencli.parser.OpenCliHelpResult;
import io.github.easy4j.opencli.parser.OpenCliHelpSchema;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class OpenCliHelpParserTest {

    @Test
    void decodesRealSnapshotsWithoutMergingScopes() {
        String raw = sample("browser-tab.json");
        OpenCliHelpResult result = OpenCliHelpParser.parse(raw);
        assertEquals(OpenCliHelpResult.Status.AVAILABLE, result.getStatus());
        assertEquals(raw, result.getRawHelp());
        OpenCliHelpSchema schema = result.getSchema();
        assertEquals("opencli browser <session> tab", schema.getCommand());
        assertEquals("tab close", schema.getCommands().get(0).getName());
        assertEquals("tab", schema.getCommands().get(0).getCommandOptions().get(0).getName());
        assertEquals("window", schema.getNamespaceOptions().get(0).getName());
        assertTrue(schema.getGlobalOptions().stream().anyMatch(option -> "profile".equals(option.getName())));
        assertTrue(schema.getSourceJson().contains("\"structured_help\""));
    }

    @Test
    void decodesRealCatalogAndRetainsAbsentMetadata() {
        OpenCliHelpSchema root = OpenCliHelpParser.parse(sample("root.json")).getSchema();
        assertEquals("opencli", root.getName());
        assertNull(root.getCommands().get(0).getCommand());
        OpenCliHelpSchema npm = OpenCliHelpParser.parse(sample("site-npm-search.json")).getSchema();
        assertEquals("npm", npm.getSite());
        assertEquals("opencli npm search", npm.getCommand());
        assertEquals(Boolean.TRUE, npm.getPositionals().get(0).getRequired());
        assertEquals("false", npm.getCommonOptions().get(2).getDefaultJson());
        assertNull(npm.getCommonOptions().get(3).getDefaultJson());
        assertEquals("required", OpenCliHelpParser.parse(sample("browser-state.json")).getSchema()
            .getNamespaceOptions().get(0).getTakesValue());
        assertEquals("npm", OpenCliHelpParser.parse(sample("site-npm.json")).getSchema().getSite());
        assertEquals("opencli auth", OpenCliHelpParser.parse(sample("meta-auth.json")).getSchema().getCommand());
        assertEquals("opencli browser <session> get",
            OpenCliHelpParser.parse(sample("browser-get.json")).getSchema().getCommand());
    }

    @Test
    void preservesFutureFieldsOptionalValuesAndExplicitFalse() {
        String raw = "{\"namespace\":\"browser\",\"name\":\"probe\",\"command\":\"opencli browser <session> probe\","
            + "\"aliases\":[\"inspect\"],\"future\":{\"version\":2,\"large\":1e400},"
            + "\"positionals\":[{\"name\":\"target\",\"variadic\":true}],"
            + "\"command_options\":[{\"name\":\"sample\",\"takes_value\":\"optional\","
            + "\"required\":false,\"negate\":true,\"default\":false,\"choices\":[\"a\",\"b\"],\"extra\":42}]}";
        OpenCliHelpResult result = OpenCliHelpParser.parse(raw);
        assertEquals(OpenCliHelpResult.Status.AVAILABLE, result.getStatus(), String.valueOf(result.getReason()));
        OpenCliHelpSchema schema = result.getSchema();
        assertEquals("inspect", schema.getAliases().get(0));
        assertThrows(UnsupportedOperationException.class, () -> schema.getAliases().add("changed"));
        assertEquals(Boolean.TRUE, schema.getPositionals().get(0).getVariadic());
        assertEquals("optional", schema.getCommandOptions().get(0).getTakesValue());
        assertEquals(Boolean.TRUE, schema.getCommandOptions().get(0).getNegate());
        assertFalse(schema.getCommandOptions().get(0).getRequired());
        assertEquals("false", schema.getCommandOptions().get(0).getDefaultJson());
        assertEquals("b", schema.getCommandOptions().get(0).getChoices().get(1));
        assertTrue(schema.getSourceJson().contains("\"future\""));
        assertTrue(schema.getSourceJson().contains("1E+400"));
        assertTrue(schema.getCommandOptions().get(0).getSourceJson().contains("\"extra\":42"));
    }

    @Test
    void returnsUnknownForNonJsonOrInvalidHelpShapes() {
        assertUnknown(sample("meta-list.txt"), OpenCliHelpResult.Reason.UNSUPPORTED_FORMAT);
        assertUnknown(sample("browser.stdout"), OpenCliHelpResult.Reason.INVALID_JSON);
        assertUnknown("{\"latest\":true}", OpenCliHelpResult.Reason.INVALID_SCHEMA);
        assertUnknown("{\"site\":\"npm\",\"site\":\"other\",\"commands\":[]}", OpenCliHelpResult.Reason.INVALID_JSON);
        assertUnknown("{\"namespace\":\"browser\",\"command\":\"opencli browser\",\"commands\":{}}",
            OpenCliHelpResult.Reason.INVALID_SCHEMA);
        assertUnknown("{\"site\":\"npm\",\"commands\":[{\"name\":17}]}",
            OpenCliHelpResult.Reason.INVALID_SCHEMA);
        assertUnknown(" ", OpenCliHelpResult.Reason.EMPTY);
    }

    private static void assertUnknown(String raw, OpenCliHelpResult.Reason reason) {
        OpenCliHelpResult result = OpenCliHelpParser.parse(raw);
        assertEquals(OpenCliHelpResult.Status.UNKNOWN, result.getStatus());
        assertEquals(reason, result.getReason());
        assertEquals(raw, result.getRawHelp());
        assertNull(result.getSchema());
    }

    private static String sample(String name) {
        InputStream stream = OpenCliHelpParserTest.class
            .getResourceAsStream("/opencli-contracts/v1/structured-help-1.8.8/" + name);
        try (Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
