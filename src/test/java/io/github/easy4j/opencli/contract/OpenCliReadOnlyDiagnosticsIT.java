package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.capability.OpenCliCapabilitySnapshot;
import io.github.easy4j.opencli.capability.OpenCliDiagnostics;
import io.github.easy4j.opencli.capability.OpenCliDiagnostics.Check;
import io.github.easy4j.opencli.capability.OpenCliDiagnostics.Status;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies version diagnostics with the installed OpenCLI package and an isolated startup hook. */
class OpenCliReadOnlyDiagnosticsIT {
    @Test
    void diagnosticsBypassTheOpenCliStartupHook(@TempDir Path home) throws IOException {
        Path entry = fixture("OPENCLI_INSTALLED_ENTRY");
        Path preload = home.resolve("isolated-home.mjs");
        write(preload, "import os from 'node:os';\n"
            + "import { syncBuiltinESMExports } from 'node:module';\n"
            + "os.homedir = () => process.env.OPENCLI_FIXTURE_HOME;\n"
            + "syncBuiltinESMExports();\n");
        Path marker = home.resolve("startup-marker");
        Path plugin = home.resolve(".opencli/plugins/diagnostics-fixture");
        Files.createDirectories(plugin);
        write(plugin.resolve("package.json"), "{\"type\":\"module\"}");
        write(plugin.resolve("startup.js"),
            "import { onStartup } from '" + entry.resolveSibling("hooks.js").toUri().toASCIIString() + "';\n"
                + "import { writeFileSync } from 'node:fs';\n"
                + "onStartup(() => writeFileSync(process.env.OPENCLI_FIXTURE_MARKER, 'ran'));\n");

        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutable(fixture("OPENCLI_INSTALLED_NODE").toString());
        properties.setLeadingArguments(Arrays.asList("--import", preload.toUri().toASCIIString(), entry.toString()));
        properties.getEnvironment().put("OPENCLI_FIXTURE_HOME", home.toString());
        properties.getEnvironment().put("OPENCLI_FIXTURE_MARKER", marker.toString());
        properties.getEnvironment().put("OPENCLI_CONFIG_DIR", home.resolve("config").toString());
        properties.getEnvironment().put("OPENCLI_CACHE_DIR", home.resolve("cache").toString());
        properties.getEnvironment().put("HOME", home.toString());
        properties.getEnvironment().put("CI", "1");
        properties.getEnvironment().put("NO_COLOR", "1");

        try (OpenCliClient client = new OpenCliClient(properties)) {
            OpenCliDiagnostics first = client.diagnose();
            assertEquals(Status.AVAILABLE, first.observation(Check.VERSION).getStatus(),
                first.observation(Check.VERSION).getReason());
            assertEquals("1.8.8", first.getVersion());
            assertEquals(Status.UNKNOWN, first.observation(Check.STRUCTURED_HELP).getStatus());
            assertFalse(Files.exists(marker));
            assertFalse(Files.exists(home.resolve("config")));

            OpenCliCapabilitySnapshot explicit = client.discoverCapabilities();
            assertEquals(OpenCliCapabilitySnapshot.Status.AVAILABLE, explicit.getStatus());
            assertTrue(Files.isRegularFile(marker), "explicit help must exercise the installed startup hook");
            Files.delete(marker);
            assertEquals(Status.STALE, client.diagnose().observation(Check.STRUCTURED_HELP).getStatus());
            assertFalse(Files.exists(marker));
        }
    }

    private static Path fixture(String name) {
        return Paths.get(Objects.requireNonNull(System.getenv(name), name + " fixture is required"));
    }

    private static void write(Path file, String text) throws IOException {
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
    }
}
