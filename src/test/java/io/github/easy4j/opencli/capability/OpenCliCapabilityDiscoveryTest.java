package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class OpenCliCapabilityDiscoveryTest {
    @Test
    void rootProbePreservesUnknownSourceAndUsesOnlyDescriptiveCommands() {
        OpenCliProperties properties = properties("structured");
        try (OpenCliClient client = new OpenCliClient(properties)) {
            OpenCliCapabilitySnapshot result = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, result.getStatus());
            assertEquals("1.8.8", result.getObservedVersion());
            assertEquals("UNKNOWN", result.getCommandOrigin());
            assertEquals("ROOT_HELP", result.getScope());
            assertNotNull(result.getObservedAt());
            assertTrue(new File(result.getExecutable()).isAbsolute());
            assertEquals(64, result.getConfigurationSha256().length());
            assertEquals(64, result.getHelpSha256().length());
            assertTrue(result.getHelp().getRawHelp().contains("do not run this example"));
            String structuredHash = result.getHelpSha256();
            properties.getEnvironment().put("OPENCLI_TEST_HELP_MODE", "revised");
            OpenCliCapabilitySnapshot revised = client.discoverCapabilities();
            assertEquals("1.8.8", revised.getObservedVersion());
            assertNotEquals(structuredHash, revised.getHelpSha256());
            assertNotEquals(result.getConfigurationSha256(), revised.getConfigurationSha256());
        }
    }

    @Test
    void plainHelpLeavesLocalInvocationAvailable() {
        try (OpenCliClient client = new OpenCliClient(properties("plain"))) {
            OpenCliCapabilitySnapshot result = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, result.getStatus());
            assertEquals("UNSUPPORTED_FORMAT", result.getReason());
            assertEquals("Usage: opencli\n", result.getHelp().getRawHelp());
            assertEquals("raw available", client.getExecutor().invoke("npm", "search").getStdout());
        }
    }

    @Test
    void malformedRootRegistryLeavesLocalInvocationAvailable() {
        try (OpenCliClient client = new OpenCliClient(properties("malformed-root"))) {
            OpenCliCapabilitySnapshot result = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, result.getStatus());
            assertEquals("INVALID_SCHEMA", result.getReason());
            assertEquals("1.8.8", result.getObservedVersion());
            assertEquals("raw available", client.getExecutor().invoke("npm", "search").getStdout());
        }
    }

    @Test
    void pathLookupAndRemoteModeAreUnsupported() {
        try (OpenCliClient client = new OpenCliClient()) {
            assertEquals("ABSOLUTE_EXECUTABLE_REQUIRED", client.discoverCapabilities().getReason());
        }
        OpenCliProperties props = properties("structured");
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        try (OpenCliClient client = new OpenCliClient(props)) {
            assertEquals("LOCAL_DISCOVERY_ONLY", client.discoverCapabilities().getReason());
        }
    }

    @Test
    void truncatedHelpAndNonzeroOutputAreDiscoveryFailures() {
        OpenCliProperties limited = properties("structured");
        limited.setMaxStdoutBytes(16);
        try (OpenCliClient client = new OpenCliClient(limited)) {
            OpenCliCapabilitySnapshot result = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, result.getStatus());
            assertEquals("HELP_TRUNCATED", result.getReason());
        }
        try (OpenCliClient client = new OpenCliClient(properties("error"))) {
            assertEquals("HELP_PROBE_FAILED", client.discoverCapabilities().getReason());
        }
    }

    @Test
    void slowHelpReturnsTimeoutWithinTheConfiguredProbeBudget() {
        OpenCliProperties limited = properties("slow");
        limited.setStartupProbeTimeoutMillis(2_000);
        try (OpenCliClient client = new OpenCliClient(limited)) {
            OpenCliCapabilitySnapshot result = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, result.getStatus());
            assertEquals("HELP_TIMEOUT", result.getReason());
            assertEquals("1.8.8", result.getObservedVersion());
        }
    }

    @Test
    void staleFallbackRetainsObservationAndRefreshEvidence(@TempDir Path directory) throws Exception {
        Path control = directory.resolve("mode");
        writeMode(control, "first");
        OpenCliProperties props = controlledProperties(control);
        try (OpenCliClient client = new OpenCliClient(props)) {
            OpenCliCapabilitySnapshot first = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, first.getStatus());

            writeMode(control, "plain");
            OpenCliCapabilitySnapshot stale = client.discoverCapabilities();
            assertEquals("STALE", stale.getStatus().name());
            assertEquals("UNSUPPORTED_FORMAT", stale.getReason());
            assertEquals(first.getObservedAt(), stale.getObservedAt());
            assertEquals(first.getHelpSha256(), stale.getHelpSha256());
            assertEquals(first.getHelp().getRawHelp(), stale.getHelp().getRawHelp());
            assertEquals("Usage: opencli\n", stale.getFailedRefresh().getHelp().getRawHelp());
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, stale.getFailedRefresh().getStatus());
            assertFalse(stale.getFailedRefresh().getObservedAt().isBefore(first.getObservedAt()));

            writeMode(control, "second");
            OpenCliCapabilitySnapshot current = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, current.getStatus());
            assertEquals(first.getConfigurationSha256(), current.getConfigurationSha256());
            assertNotEquals(first.getHelpSha256(), current.getHelpSha256());

            writeMode(control, "error");
            OpenCliCapabilitySnapshot staleAfterChange = client.discoverCapabilities();
            assertEquals("STALE", staleAfterChange.getStatus().name());
            assertEquals(current.getHelpSha256(), staleAfterChange.getHelpSha256());
        }
    }

    @Test
    void configAndVersionChangesInvalidateCachedHelp(@TempDir Path directory) throws Exception {
        Path control = directory.resolve("mode");
        writeMode(control, "baseline");
        OpenCliProperties props = controlledProperties(control);
        try (OpenCliClient client = new OpenCliClient(props)) {
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, client.discoverCapabilities().getStatus());
            writeMode(control, "new-version-error");
            OpenCliCapabilitySnapshot upgraded = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, upgraded.getStatus());
            assertEquals("1.8.9", upgraded.getObservedVersion());
            assertNull(upgraded.getHelp());

            writeMode(control, "error");
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, client.discoverCapabilities().getStatus());
            writeMode(control, "baseline");
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, client.discoverCapabilities().getStatus());
            writeMode(control, "error");
            props.getEnvironment().put("OPENCLI_TEST_CHANGED_CONFIG", "true");
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, client.discoverCapabilities().getStatus());
            props.getEnvironment().remove("OPENCLI_TEST_CHANGED_CONFIG");
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, client.discoverCapabilities().getStatus());
        }
    }

    @Test
    void lateRefreshCannotOverwriteNewerSnapshot(@TempDir Path directory) throws Exception {
        Path control = directory.resolve("mode");
        Path entered = directory.resolve("entered");
        Path release = directory.resolve("release");
        writeMode(control, "hold-old");
        OpenCliProperties props = controlledProperties(control);
        props.setMaxConcurrentExecutions(2);
        ExecutorService callers = Executors.newSingleThreadExecutor();
        try (OpenCliClient client = new OpenCliClient(props)) {
            Future<OpenCliCapabilitySnapshot> older = callers.submit(() -> client.discoverCapabilities());
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!Files.exists(entered) && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertTrue(Files.exists(entered), "older probe must be held after it reads the old help mode");
            writeMode(control, "newer");
            OpenCliCapabilitySnapshot newest = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, newest.getStatus());

            Files.createFile(release);
            OpenCliCapabilitySnapshot olderObservation = older.get(5, TimeUnit.SECONDS);
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, olderObservation.getStatus());
            assertNotEquals(newest.getHelpSha256(), olderObservation.getHelpSha256());
            writeMode(control, "error");
            OpenCliCapabilitySnapshot stale = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.STALE, stale.getStatus());
            assertEquals(newest.getHelpSha256(), stale.getHelpSha256());
        } finally {
            callers.shutdownNow();
        }
    }

    @Test
    void fixedQueriesReturnTheirCapturedScopeAndKeepUnsupportedFormats() {
        OpenCliHelpScope[] scopes = {OpenCliHelpScope.BROWSER_TAB, OpenCliHelpScope.BROWSER_GET,
            OpenCliHelpScope.BROWSER_STATE, OpenCliHelpScope.META_AUTH, OpenCliHelpScope.SITE_NPM,
            OpenCliHelpScope.SITE_NPM_SEARCH};
        try (OpenCliClient client = new OpenCliClient(properties("structured"))) {
            for (OpenCliHelpScope scope : scopes) {
                OpenCliCapabilitySnapshot snapshot = client.discoverCapabilities(scope);
                assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, snapshot.getStatus(), scope.name());
                assertEquals(scope.name(), snapshot.getScope());
                assertEquals("UNKNOWN", snapshot.getCommandOrigin());
            }
            assertEquals("opencli browser <session> state",
                client.discoverCapabilities(OpenCliHelpScope.BROWSER_STATE).getHelp().getSchema().getCommand());
            assertEquals("UNSUPPORTED_FORMAT", client.discoverCapabilities(OpenCliHelpScope.META_LIST).getReason());
            assertEquals("INVALID_JSON", client.discoverCapabilities(OpenCliHelpScope.BROWSER).getReason());
        }
    }

    @Test
    void freshnessIsScopedAndVersionChangesClearOldQueries(@TempDir Path directory) throws Exception {
        Path control = directory.resolve("mode");
        writeMode(control, "structured");
        try (OpenCliClient client = new OpenCliClient(controlledProperties(control))) {
            OpenCliCapabilitySnapshot root = client.discoverCapabilities();
            OpenCliCapabilitySnapshot state = client.discoverCapabilities(OpenCliHelpScope.BROWSER_STATE);
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, state.getStatus());

            writeMode(control, "wrong-help");
            OpenCliCapabilitySnapshot stale = client.discoverCapabilities(OpenCliHelpScope.BROWSER_STATE);
            assertEquals(OpenCliCapabilitySnapshot.Status.STALE, stale.getStatus());
            assertEquals("UNEXPECTED_HELP_SCOPE", stale.getReason());
            assertEquals(state.getHelpSha256(), stale.getHelpSha256());
            assertNotEquals(root.getHelpSha256(), stale.getHelpSha256());

            writeMode(control, "new-version-error");
            OpenCliCapabilitySnapshot upgraded = client.discoverCapabilities(OpenCliHelpScope.BROWSER_STATE);
            assertEquals(OpenCliCapabilitySnapshot.Status.UNSUPPORTED, upgraded.getStatus());
            assertEquals("UNVERIFIED_HELP_VERSION", upgraded.getReason());
            writeMode(control, "error");
            assertEquals(OpenCliCapabilitySnapshot.Status.UNKNOWN, client.discoverCapabilities().getStatus());
        }
    }

    private static OpenCliProperties controlledProperties(Path control) {
        OpenCliProperties props = properties("unused");
        props.getEnvironment().put("OPENCLI_TEST_HELP_CONTROL", control.toString());
        return props;
    }

    private static void writeMode(Path control, String mode) throws Exception {
        Files.write(control, mode.getBytes(StandardCharsets.UTF_8));
    }

    private static OpenCliProperties properties(String mode) {
        OpenCliProperties props = new OpenCliProperties();
        String binary = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        props.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), binary).getAbsolutePath());
        props.setLeadingArguments(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            DiscoveryFixture.class.getName()));
        props.getEnvironment().put("OPENCLI_TEST_HELP_MODE", mode);
        return props;
    }

    public static class DiscoveryFixture {
        public static void main(String[] args) throws Exception {
            String mode = System.getenv("OPENCLI_TEST_HELP_MODE");
            String control = System.getenv("OPENCLI_TEST_HELP_CONTROL");
            if (Objects.nonNull(control)) {
                mode = new String(Files.readAllBytes(Paths.get(control)), StandardCharsets.UTF_8);
            }
            if (Arrays.equals(args, new String[]{"--version"})) {
                System.out.println("new-version-error".equals(mode) ? "1.8.9" : "1.8.8");
                return;
            }
            if (Arrays.equals(args, new String[]{"npm", "search"})) {
                System.out.print("raw available");
                return;
            }
            if (args.length > 3 && Arrays.asList("--help", "-f", "json")
                .equals(Arrays.asList(args).subList(args.length - 3, args.length))) {
                if ("new-version-error".equals(mode) || "error".equals(mode)) {
                    System.exit(3);
                }
                String path = String.join(" ", Arrays.asList(args).subList(0, args.length - 3));
                String fixture;
                switch (path) {
                    case "browser": fixture = "browser.stdout"; break;
                    case "browser fixture tab": fixture = "browser-tab.json"; break;
                    case "browser fixture get": fixture = "browser-get.json"; break;
                    case "browser fixture state": fixture = "browser-state.json"; break;
                    case "auth": fixture = "meta-auth.json"; break;
                    case "list": fixture = "meta-list.txt"; break;
                    case "npm": fixture = "site-npm.json"; break;
                    case "npm search": fixture = "site-npm-search.json"; break;
                    default: throw new IllegalArgumentException("Unapproved fixture command");
                }
                System.out.print(readFixture("wrong-help".equals(mode) ? "root.json" : fixture));
                return;
            }
            if (!Arrays.equals(args, new String[]{"--help", "-f", "json"})) {
                System.exit(2);
            }
            if ("hold-old".equals(mode)) {
                Path path = Paths.get(control).getParent();
                Files.createFile(path.resolve("entered"));
                while (!Files.exists(path.resolve("release"))) {
                    Thread.sleep(10);
                }
            }
            if ("error".equals(mode) || "new-version-error".equals(mode)) {
                System.exit(3);
            }
            if ("slow".equals(mode)) {
                Thread.sleep(30_000);
            }
            if ("plain".equals(mode)) {
                System.out.print("Usage: opencli\n");
                return;
            }
            if ("malformed-root".equals(mode)) {
                System.out.print("{\"name\":\"opencli\",\"commands\":[],\"site_adapters\":{}}");
                return;
            }
            String root = readFixture("root.json").trim();
            System.out.print(root.substring(0, root.length() - 1)
                + ",\"description_fixture\":\"do not run this example " + mode + "\"}");
        }

        private static String readFixture(String fixture) {
            try (InputStream input = DiscoveryFixture.class.getResourceAsStream(
                "/opencli-contracts/v1/structured-help-1.8.8/" + fixture);
                Scanner scan = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
                return scan.next();
            } catch (java.io.IOException ex) {
                throw new java.io.UncheckedIOException(ex);
            }
        }
    }
}
