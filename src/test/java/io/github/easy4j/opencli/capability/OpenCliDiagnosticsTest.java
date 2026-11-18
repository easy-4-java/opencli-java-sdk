package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.capability.OpenCliDiagnostics.Check;
import io.github.easy4j.opencli.capability.OpenCliDiagnostics.Source;
import io.github.easy4j.opencli.capability.OpenCliDiagnostics.Status;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class OpenCliDiagnosticsTest {
    @Test
    void versionProbeDoesNotRunHelpDoctorAuthOrDaemonCommands(@TempDir Path temp) throws Exception {
        Path log = temp.resolve("commands");
        try (OpenCliClient client = new OpenCliClient(properties(log))) {
            OpenCliDiagnostics diagnostics = client.diagnose();
            assertEquals(Status.AVAILABLE, diagnostics.observation(Check.EXECUTABLE).getStatus());
            assertEquals(Status.AVAILABLE, diagnostics.observation(Check.VERSION).getStatus());
            assertEquals("1.8.8", diagnostics.getVersion());
            assertEquals(Status.UNKNOWN, diagnostics.observation(Check.STRUCTURED_HELP).getStatus());
            assertEquals(Status.UNKNOWN, diagnostics.observation(Check.BROWSER_BRIDGE).getStatus());
            assertEquals(Status.UNKNOWN, diagnostics.observation(Check.AUTHENTICATION).getStatus());
            assertEquals(Status.UNKNOWN, diagnostics.observation(Check.DAEMON).getStatus());
            assertEquals(Status.NOT_APPLICABLE, diagnostics.observation(Check.REMOTE_TRANSPORT).getStatus());
            assertEquals(Source.VERSION_PROBE, diagnostics.observation(Check.VERSION).getSource());
            assertNotNull(diagnostics.observation(Check.VERSION).getObservedAt());
            assertEquals(Collections.singletonList("--version"), Files.readAllLines(log));
        }
    }

    @Test
    void failedHelpRemainsUnknownAndSuccessfulPriorHelpIsHistorical(@TempDir Path temp) throws Exception {
        Path log = temp.resolve("commands");
        Path control = temp.resolve("mode");
        OpenCliProperties props = properties(log);
        props.getEnvironment().put("OPENCLI_FIXTURE_CONTROL", control.toString());
        Files.write(control, new byte[] {'F'});
        try (OpenCliClient client = new OpenCliClient(props)) {
            OpenCliCapabilitySnapshot failed = client.discoverCapabilities();
            assertEquals("UNSUPPORTED_FORMAT", failed.getReason());
            OpenCliDiagnostics first = client.diagnose();
            assertEquals(Status.AVAILABLE, first.observation(Check.VERSION).getStatus());
            assertEquals(Status.UNKNOWN, first.observation(Check.STRUCTURED_HELP).getStatus());
            assertEquals("UNSUPPORTED_FORMAT", first.observation(Check.STRUCTURED_HELP).getReason());
            assertEquals(failed.getObservedAt(), first.observation(Check.STRUCTURED_HELP).getObservedAt());

            Files.write(control, new byte[] {'S'});
            OpenCliCapabilitySnapshot successful = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, successful.getStatus());
            OpenCliDiagnostics next = client.diagnose();
            assertEquals(Status.STALE, next.observation(Check.STRUCTURED_HELP).getStatus());
            assertEquals(Source.PREVIOUS_DISCOVERY, next.observation(Check.STRUCTURED_HELP).getSource());
            assertEquals(successful.getObservedAt(), next.observation(Check.STRUCTURED_HELP).getObservedAt());

            props.getEnvironment().put("OPENCLI_CHANGED_CONFIG", "1");
            assertEquals(Status.UNKNOWN, client.diagnose().observation(Check.STRUCTURED_HELP).getStatus());
            assertEquals(Arrays.asList("--version", "--help -f json", "--version",
                "--version", "--help -f json", "--version", "--version"), Files.readAllLines(log));
        }
    }

    @Test
    void remoteModeSkipsLocalProbesAndRelativeExecutablesAreUnverified(@TempDir Path temp) {
        Path log = temp.resolve("commands");
        OpenCliProperties props = properties(log);
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        try (OpenCliClient client = new OpenCliClient(props)) {
            OpenCliDiagnostics result = client.diagnose();
            assertEquals(Status.NOT_APPLICABLE, result.observation(Check.VERSION).getStatus());
            assertEquals(Status.NOT_APPLICABLE, result.observation(Check.DAEMON).getStatus());
            assertEquals(Status.UNKNOWN, result.observation(Check.REMOTE_TRANSPORT).getStatus());
            assertFalse(Files.exists(log));
        }
        try (OpenCliClient client = new OpenCliClient()) {
            assertEquals(Status.UNKNOWN, client.diagnose().observation(Check.EXECUTABLE).getStatus());
        }
    }

    private static OpenCliProperties properties(Path log) {
        OpenCliProperties result = new OpenCliProperties();
        String binary = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        result.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), binary).getAbsolutePath());
        result.setLeadingArguments(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            Child.class.getName(), log.toString()));
        return result;
    }

    public static class Child {
        public static void main(String[] argv) throws Exception {
            String command = String.join(" ", Arrays.asList(argv).subList(1, argv.length));
            Files.write(Paths.get(argv[0]), Collections.singletonList(command), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if ("--version".equals(command)) {
                System.out.println("1.8.8");
            } else if ("--help -f json".equals(command)) {
                String control = System.getenv("OPENCLI_FIXTURE_CONTROL");
                if (control != null && Files.readAllBytes(Paths.get(control))[0] == 'S') {
                    try (InputStream fixture = Child.class.getResourceAsStream(
                             "/opencli-contracts/v1/structured-help-1.8.8/root.json");
                         Scanner scanner = new Scanner(fixture, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
                        System.out.print(scanner.next());
                    }
                } else {
                    System.out.println("Usage: opencli");
                }
            } else {
                throw new AssertionError("Unapproved diagnostic command");
            }
        }
    }
}
