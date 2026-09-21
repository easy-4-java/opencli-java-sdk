package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.browser.OpenCliBrowserClient;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.core.OpenCliAdapterCommandRequest;
import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import static org.junit.jupiter.api.Assertions.*;

/** Shared Java 8-compatible contract tests, with a real child rather than Recording executor. */
class OpenCliArgvContractTest {
    private static OpenCliProperties properties() {
        OpenCliProperties p = new OpenCliProperties();
        String exe = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        p.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), exe).getAbsolutePath());
        p.setLeadingArguments(new ArrayList<>(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            ContractProbe.class.getName())));
        p.setCommandTimeoutMillis(10000L);
        return p;
    }

    private static List<String> received(OpenCliResult result) throws Exception {
        assertTrue(result.isSuccess());
        List<String> actual = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(result.getStdout()))) {
            String header = reader.readLine();
            assertNotNull(header, "child produced no argv evidence");
            assertTrue(header.startsWith("argc:"), "missing child protocol header");
            int count = Integer.parseInt(header.substring(5));
            for (int i = 0; i < count; i++) {
                String line = reader.readLine();
                assertNotNull(line, "incomplete child output");
                assertTrue(line.startsWith("arg:"));
                actual.add(new String(Base64.getDecoder().decode(line.substring(4)), StandardCharsets.UTF_8));
            }
            assertNull(reader.readLine(), "unexpected extra child output");
        }
        return actual;
    }

    @TestFactory
    Stream<DynamicTest> sharedVectorsReachEveryRawEntryPoint() throws Exception {
        List<DynamicTest> tests = new ArrayList<>();
        InputStream resource = getClass().getResourceAsStream("/opencli-contracts/v1/argv.tsv");
        assertNotNull(resource, "shared fixture is required");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                List<String> expected = new ArrayList<>();
                for (int i = 1; i < fields.length; i++) {
                    expected.add(new String(Base64.getDecoder().decode(fields[i]), StandardCharsets.UTF_8));
                }
                for (int mode = 0; mode < 4; mode++) {
                    final int entry = mode;
                    tests.add(DynamicTest.dynamicTest(fields[0] + "/entry-" + mode, () -> {
                        OpenCliExecutor executor = new OpenCliExecutor(properties());
                        List<String> before = new ArrayList<>(expected);
                        OpenCliResult result;
                        if (entry == 0) {
                            result = executor.invoke(expected);
                        } else if (entry == 1) {
                            result = executor.invoke(expected.toArray(new String[0]));
                        } else {
                            OpenCliAdapterChannel channel = new OpenCliAdapterChannel(executor, expected.get(0));
                            List<String> rest = expected.subList(1, expected.size());
                            result = entry == 2 ? channel.invoke(rest) : channel.invoke(rest.toArray(new String[0]));
                        }
                        assertEquals(before, expected, "caller list changed");
                        assertEquals(expected, received(result), "full argv changed at child boundary");
                    }));
                }
            }
        }
        assertEquals(24, tests.size(), "fixture denominator changed; review sources.lock.json");
        return tests.stream();
    }

    @Test
    void structuredPositionalsAndValuesReachChildUnchanged() throws Exception {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("text", "  value  ");
        OpenCliAdapterCommandRequest request = OpenCliAdapterCommandRequest.builder()
            .subcommand("echo").positional("").positional("  positional  ").options(options).build();
        assertEquals(Arrays.asList("demo", "echo", "", "  positional  ", "--text", "  value  "),
            received(new OpenCliAdapterChannel(new OpenCliExecutor(properties()), "demo").invoke(request)));
    }

    @Test
    void typedBrowserFillCanClearAField() throws Exception {
        OpenCliBrowserClient browser = new OpenCliBrowserClient(new OpenCliExecutor(properties()));
        assertEquals(Arrays.asList("browser", "contract", "fill", "#input", ""),
            received(browser.session("contract").fill("#input", "", null, null)));
    }

    @Test
    void rawMergePreservesEmptyAndPaddedValues() throws Exception {
        List<String> prefix = Arrays.asList("demo", "echo", "");
        List<String> extra = Arrays.asList("  x  ", "--", "-literal");
        assertEquals(Arrays.asList("demo", "echo", "", "  x  ", "--", "-literal"),
            received(new OpenCliExecutor(properties()).invoke(OpenCliArgSupport.merge(prefix, extra))));
        assertEquals(Arrays.asList("demo", "echo", ""), prefix);
    }

    @Test
    void leadingArgumentsPreserveEmptyAndPaddedValues() throws Exception {
        OpenCliProperties p = properties();
        p.getLeadingArguments().add("");
        p.getLeadingArguments().add("  leading  ");
        assertEquals(Arrays.asList("", "  leading  ", "demo"),
            received(new OpenCliExecutor(p).invoke("demo")));
    }

    @Test
    void nullRawTokenIsRejectedWithoutLeakingOtherTokens() {
        OpenCliExecutor executor = new OpenCliExecutor(properties());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> executor.invoke(Arrays.asList("demo", "SECRET_MARKER", null)));
        assertTrue(error.getMessage().contains("2"), "validation must identify null index");
        assertFalse(error.getMessage().contains("SECRET_MARKER"));
    }

    @Test
    void blankCommandIdentifierIsRejectedBeforeSpawn() {
        OpenCliExecutor executor = new OpenCliExecutor(properties());
        assertThrows(IllegalArgumentException.class, () -> executor.invoke(Arrays.asList("", "demo")));
        assertThrows(IllegalArgumentException.class, () -> OpenCliAdapterCommandRequest.builder()
            .subcommand(" ").build().toSubcommandAndArgs());
    }

    @Test
    void nullVarargsContainerIsRejectedConsistently() {
        OpenCliExecutor executor = new OpenCliExecutor(properties());
        assertThrows(NullPointerException.class, () -> executor.invoke((String[]) null));
        assertThrows(NullPointerException.class,
            () -> new OpenCliAdapterChannel(executor, "demo").invoke((String[]) null));
    }

    @Test
    void nullLeadingTokenIsRejectedBeforeSpawn() {
        OpenCliProperties p = properties();
        p.getLeadingArguments().add(null);
        assertThrows(IllegalArgumentException.class, () -> new OpenCliExecutor(p).invoke("demo"));
    }

    @Test
    void nullMergedTokenIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliArgSupport.merge(Arrays.asList("demo", null), Collections.emptyList()));
    }
}
