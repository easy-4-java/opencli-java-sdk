package io.github.easy4j.opencli.contract;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenCliInstalledLauncherIT {

    @Test
    void nativeNodeLaunchReachesInstalledOpenCli() throws Exception {
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutable(requireFixture("OPENCLI_INSTALLED_NODE").toString());
        properties.setLeadingArguments(Collections.singletonList(requireFixture("OPENCLI_INSTALLED_ENTRY").toString()));
        properties.setCommandTimeoutMillis(30_000L);

        try (OpenCliClient client = new OpenCliClient(properties)) {
            OpenCliResult result = client.meta().list("json");
            assertTrue(result.isSuccess());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actual = mapper.readTree(result.getStdout());
            JsonNode expected = mapper.readTree(requireFixture("OPENCLI_INSTALLED_REGISTRY").toFile());
            assertTrue(actual.isArray());
            assertTrue(actual.size() > 0);
            assertEquals(expected, actual);
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void installedNpmBatchShimIsRejectedExplicitly() {
        File shim = requireFixture("OPENCLI_INSTALLED_BATCH_SHIM").toFile();
        assertTrue(shim.isFile(), "npm must provide the Windows batch shim");
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutable(shim.getAbsolutePath());
        try (OpenCliClient client = new OpenCliClient(properties)) {
            UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
                () -> client.meta().list("json"));
            assertTrue(error.getMessage().contains("native node executable"));
        }
    }

    private static Path requireFixture(String name) {
        return Paths.get(Objects.requireNonNull(System.getenv(name), name + " fixture is required"));
    }
}
