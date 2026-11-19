package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliInvocationContext;
import io.github.easy4j.opencli.OpenCliInvocationContext.SiteSession;
import io.github.easy4j.opencli.OpenCliInvocationContext.WindowMode;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class OpenCliInvocationContextTest {
    @Test
    void concurrentInvocationsKeepTheirProfileAndWindow() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try (OpenCliClient client = new OpenCliClient(properties())) {
            Future<String> a = pool.submit(() -> state(client, "profile-a", WindowMode.BACKGROUND));
            Future<String> b = pool.submit(() -> state(client, "profile-b", WindowMode.FOREGROUND));
            assertEquals("--profile|profile-a|browser|session-a|--window|background|state", a.get(12, TimeUnit.SECONDS));
            assertEquals("--profile|profile-b|browser|session-a|--window|foreground|state", b.get(12, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void failedInvocationKeepsItsContextWithoutChangingConcurrentOrDefaultCalls(@TempDir Path coordination) throws Exception {
        OpenCliProperties properties = properties();
        properties.setInvocationDefaults(OpenCliInvocationContext.builder().profile("profile-default")
            .window(WindowMode.FOREGROUND).build());
        properties.getEnvironment().put("OPENCLI_CONTEXT_TEST_COORDINATION", coordination.toString());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try (OpenCliClient client = new OpenCliClient(properties)) {
            Future<OpenCliNonZeroExitException> failure = pool.submit(() -> assertThrows(
                OpenCliNonZeroExitException.class, () -> state(client, "profile-failure", WindowMode.BACKGROUND)));
            Future<String> success = pool.submit(() -> state(client, "profile-success", WindowMode.FOREGROUND));

            OpenCliNonZeroExitException failed = failure.get(20, TimeUnit.SECONDS);
            assertEquals(Integer.valueOf(7), failed.getPartialResult().getExitCode());
            assertEquals("--profile|profile-failure|browser|session-a|--window|background|state",
                failed.getPartialResult().getStdout());
            assertFalse(success.isDone());
            Files.createFile(coordination.resolve("release-peer"));
            assertEquals("--profile|profile-success|browser|session-a|--window|foreground|state",
                success.get(20, TimeUnit.SECONDS));
            assertEquals("--profile|profile-default|browser|session-a|--window|foreground|state",
                client.invokeDiscovered(OpenCliHelpScope.BROWSER_STATE, "opencli browser <session> state",
                    "session-a", null, null).getStdout());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void explicitFalseOverridesClientDefaultWithinVerifiedBrowserAdapter() {
        OpenCliProperties properties = properties();
        properties.setInvocationDefaults(OpenCliInvocationContext.builder().keepTab(true)
            .siteSession(SiteSession.EPHEMERAL).build());
        try (OpenCliClient client = new OpenCliClient(properties)) {
            OpenCliInvocationContext override = OpenCliInvocationContext.builder().keepTab(false).profile("local").build();
            assertEquals("--profile|local|zhihu|hot|--site-session|ephemeral|--keep-tab|false",
                client.invokeDiscovered(OpenCliHelpScope.SITE_ZHIHU_HOT, "opencli zhihu hot", null, null, null, override)
                    .getStdout());
            assertEquals("zhihu|hot|--site-session|ephemeral|--keep-tab|true",
                client.invokeDiscovered(OpenCliHelpScope.SITE_ZHIHU_HOT, "opencli zhihu hot", null, null, null)
                    .getStdout());
        }
    }

    @Test
    void rejectsUnsupportedScopesAndConflictingExplicitOptions() {
        try (OpenCliClient client = new OpenCliClient(properties())) {
            assertThrows(IllegalArgumentException.class, () -> client.invokeDiscovered(OpenCliHelpScope.SITE_NPM_SEARCH,
                "opencli npm search", null, Collections.singletonList("fixture"), null,
                OpenCliInvocationContext.builder().siteSession(SiteSession.PERSISTENT).build()));
            assertThrows(IllegalArgumentException.class, () -> client.invokeDiscovered(OpenCliHelpScope.BROWSER_STATE,
                "opencli browser <session> state", "session-a", null, Collections.singletonMap("--window", "foreground"),
                OpenCliInvocationContext.builder().window(WindowMode.BACKGROUND).build()));
        }
    }

    private static String state(OpenCliClient client, String profile, WindowMode window) {
        return client.invokeDiscovered(OpenCliHelpScope.BROWSER_STATE, "opencli browser <session> state",
            "session-a", null, null, OpenCliInvocationContext.builder().profile(profile).window(window).build()).getStdout();
    }

    private static OpenCliProperties properties() {
        OpenCliProperties p = new OpenCliProperties();
        String binary = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        p.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), binary).getAbsolutePath());
        p.setLeadingArguments(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")), Child.class.getName()));
        return p;
    }

    public static class Child {
        public static void main(String[] args) throws Exception {
            String argv = String.join(" ", args);
            switch (argv) {
                case "--version":
                    System.out.println("1.8.8");
                    return;
                case "browser fixture state --help -f json":
                    printFixture("browser-state");
                    return;
                case "zhihu hot --help -f json":
                    printFixture("site-zhihu-hot");
                    return;
                case "profile --help -f json":
                    printFixture("meta-profile");
                    return;
                case "npm search --help -f json":
                    printFixture("site-npm-search");
                    return;
                default:
                    if (argv.contains("npm search") || argv.contains("profile use")) {
                        System.exit(4);
                    }
                    System.out.print(String.join("|", args));
                    if (argv.startsWith("--profile profile-failure ")) {
                        Path coordination = Paths.get(System.getenv("OPENCLI_CONTEXT_TEST_COORDINATION"));
                        awaitFile(coordination.resolve("peer-ready"));
                        System.exit(7);
                    }
                    if (argv.startsWith("--profile profile-success ")) {
                        Path coordination = Paths.get(System.getenv("OPENCLI_CONTEXT_TEST_COORDINATION"));
                        Files.createFile(coordination.resolve("peer-ready"));
                        awaitFile(coordination.resolve("release-peer"));
                    }
            }
        }

        private static void awaitFile(Path file) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!Files.exists(file)) {
                if (System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Timed out waiting for peer fixture signal");
                }
                Thread.sleep(20);
            }
        }

        private static void printFixture(String name) throws Exception {
            try (InputStream stream = Child.class.getResourceAsStream(
                "/opencli-contracts/v1/structured-help-1.8.8/" + name + ".json");
                 Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
                System.out.print(scanner.next());
            }
        }
    }
}
