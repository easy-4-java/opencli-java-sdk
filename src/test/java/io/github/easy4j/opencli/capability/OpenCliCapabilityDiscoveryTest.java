package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

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
            if (Arrays.equals(args, new String[]{"--version"})) {
                System.out.println("1.8.8");
                return;
            }
            if (Arrays.equals(args, new String[]{"npm", "search"})) {
                System.out.print("raw available");
                return;
            }
            if (!Arrays.equals(args, new String[]{"--help", "-f", "json"})) {
                System.exit(2);
            }
            String mode = System.getenv("OPENCLI_TEST_HELP_MODE");
            if ("error".equals(mode)) {
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
            try (InputStream input = DiscoveryFixture.class.getResourceAsStream(
                "/opencli-contracts/v1/structured-help-1.8.8/root.json");
                Scanner scan = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
                String root = scan.next().trim();
                System.out.print(root.substring(0, root.length() - 1)
                    + ",\"description_fixture\":\"do not run this example " + mode + "\"}");
            }
        }
    }
}
