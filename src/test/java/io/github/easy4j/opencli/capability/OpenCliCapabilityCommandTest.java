package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.parser.OpenCliHelpParser;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenCliCapabilityCommandTest {
    private static final String PATH = "opencli browser <session> state";

    @Test
    void bindsSessionAndPreservesOptionalPresenceNegationAndExplicitFalse() {
        OpenCliCapabilitySnapshot snapshot = snapshot();
        Map<String, String> options = new LinkedHashMap<>();
        options.put("--window", "background");
        options.put("--label", null);
        options.put("--no-cache", null);
        options.put("--switch", "false");
        List<String> args = OpenCliCapabilityCommand.bind(snapshot, PATH, "session-a",
            Arrays.asList("first", "--second"), options);
        assertEquals(Arrays.asList("browser", "session-a", "--window", "background", "state",
            "--label", "--no-cache", "--switch", "false", "--", "first", "--second"), args);
    }

    @Test
    void rejectsInvalidChoiceMissingSessionAndRequiredValue() {
        OpenCliCapabilitySnapshot snapshot = snapshot();
        List<String> positionals = Collections.singletonList("target");
        Map<String, String> invalid = Collections.singletonMap("--switch", "invalid");
        assertTrue(assertThrows(IllegalArgumentException.class,
            () -> OpenCliCapabilityCommand.bind(snapshot, PATH, "s", positionals, invalid))
            .getMessage().contains("switch"));
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCapabilityCommand.bind(snapshot, PATH, null, positionals, Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCapabilityCommand.bind(snapshot, PATH, "--version", positionals, Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCapabilityCommand.bind(snapshot, PATH, "s", positionals, Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliCapabilityCommand.bind(snapshot, PATH, "s", Collections.emptyList(),
                Collections.singletonMap("--switch", "false")));
        assertThrows(IllegalStateException.class,
            () -> OpenCliCapabilityCommand.bind(snapshot.toBuilder().status(OpenCliCapabilitySnapshot.Status.STALE).build(),
                PATH, "s", positionals, Collections.singletonMap("--switch", "false")));
    }

    private static OpenCliCapabilitySnapshot snapshot() {
        return OpenCliCapabilitySnapshot.builder().status(OpenCliCapabilitySnapshot.Status.AVAILABLE)
            .help(OpenCliHelpParser.parse("{\"namespace\":\"browser\",\"name\":\"state\","
                + "\"command\":\"opencli browser <session> state\","
                + "\"positionals\":[{\"name\":\"target\",\"required\":true},{\"name\":\"rest\",\"variadic\":true}],"
                + "\"namespace_options\":[{\"name\":\"window\",\"flags\":\"--window <mode>\"}],"
                + "\"command_options\":[{\"name\":\"label\",\"flags\":\"--label [value]\",\"takes_value\":\"optional\"},"
                + "{\"name\":\"cache\",\"flags\":\"--no-cache\",\"negate\":true},"
                + "{\"name\":\"switch\",\"flags\":\"--switch <enabled>\",\"required\":true,"
                + "\"choices\":[\"true\",\"false\"]}]}")).build();
    }
}
