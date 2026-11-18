package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.capability.OpenCliCapabilitySnapshot;
import io.github.easy4j.opencli.capability.OpenCliCapabilitySnapshot.Status;
import io.github.easy4j.opencli.capability.OpenCliHelpScope;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in real-package fixture, using the same launcher inputs as OpenCliInstalledLauncherIT. */
class OpenCliCapabilityOverrideIT {
    @Test
    void localAndPluginOverridesRefreshWithoutClaimingOriginOrExecutingHandler(@TempDir Path home) throws IOException {
        Path entry = requireFixture("OPENCLI_INSTALLED_ENTRY");
        Path preload = home.resolve("isolated-home.mjs");
        write(preload, "import os from 'node:os';\n"
            + "import { syncBuiltinESMExports } from 'node:module';\n"
            + "os.homedir = () => process.env.OPENCLI_FIXTURE_HOME;\n"
            + "syncBuiltinESMExports();\n");
        Path marker = home.resolve("business-handler-ran");
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutable(requireFixture("OPENCLI_INSTALLED_NODE").toString());
        properties.setLeadingArguments(Arrays.asList("--import", preload.toString(), entry.toString()));
        properties.getEnvironment().put("OPENCLI_FIXTURE_HOME", home.toString());
        properties.getEnvironment().put("OPENCLI_CONFIG_DIR", home.resolve("config").toString());
        properties.getEnvironment().put("OPENCLI_CACHE_DIR", home.resolve("cache").toString());
        properties.getEnvironment().put("OPENCLI_FIXTURE_MARKER", marker.toString());
        properties.getEnvironment().put("HOME", home.toString());
        properties.getEnvironment().put("CI", "1");
        properties.getEnvironment().put("NO_COLOR", "1");

        Path sdkHome = home.resolve(".opencli");
        try (OpenCliClient client = new OpenCliClient(properties)) {
            OpenCliCapabilitySnapshot bundled = discover(client);
            assertFalse(bundled.getHelp().getRawHelp().contains("fixture metadata"));

            installOverride(sdkHome.resolve("clis/npm"), entry, "local fixture metadata");
            OpenCliCapabilitySnapshot local = discover(client);
            assertTrue(local.getHelp().getRawHelp().contains("local fixture metadata"));
            assertNotEquals(bundled.getHelpSha256(), local.getHelpSha256());

            Path plugin = installOverride(sdkHome.resolve("plugins/fixture"), entry, "plugin fixture metadata");
            OpenCliCapabilitySnapshot overridden = discover(client);
            assertTrue(overridden.getHelp().getRawHelp().contains("plugin fixture metadata"));
            assertFalse(overridden.getHelp().getRawHelp().contains("local fixture metadata"));
            assertEquals(bundled.getConfigurationSha256(), overridden.getConfigurationSha256());
            assertNotEquals(local.getHelpSha256(), overridden.getHelpSha256());

            Files.delete(plugin);
            assertEquals(local.getHelpSha256(), discover(client).getHelpSha256());
            assertFalse(Files.exists(marker), "metadata discovery must not invoke the adapter handler");
        }
    }

    private static OpenCliCapabilitySnapshot discover(OpenCliClient client) {
        OpenCliCapabilitySnapshot snapshot = client.discoverCapabilities(OpenCliHelpScope.SITE_NPM_SEARCH);
        assertEquals(Status.AVAILABLE, snapshot.getStatus(), snapshot.getReason());
        assertEquals("1.8.8", snapshot.getObservedVersion());
        assertEquals("UNKNOWN", snapshot.getCommandOrigin());
        assertEquals("SITE_NPM_SEARCH", snapshot.getScope());
        assertEquals("opencli npm search", snapshot.getHelp().getSchema().getCommand());
        return snapshot;
    }

    private static Path installOverride(Path directory, Path entry, String description) throws IOException {
        Files.createDirectories(directory);
        write(directory.resolve("package.json"), "{\"type\":\"module\"}");
        Path override = directory.resolve("search.js");
        write(override, "import { cli, Strategy } from '" + entry.resolveSibling("registry.js").toUri() + "';\n"
            + "import { writeFileSync } from 'node:fs';\n"
            + "cli({site:'npm', name:'search', strategy:Strategy.PUBLIC, browser:false, access:'read',\n"
            + "description:'" + description + "',\n"
            + "args:[{name:'query', positional:true, required:true}],\n"
            + "async func() { writeFileSync(process.env.OPENCLI_FIXTURE_MARKER, 'executed'); return []; }});\n");
        return override;
    }

    private static void write(Path file, String text) throws IOException {
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
    }

    private static Path requireFixture(String name) {
        return Paths.get(Objects.requireNonNull(System.getenv(name), name + " fixture is required"));
    }
}
