package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.browser.OpenCliBrowserSession;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserDecoder;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserTab;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliTimeoutException;
import io.github.easy4j.opencli.parser.OpenCliErrorEnvelope;
import io.github.easy4j.opencli.parser.OpenCliErrorDecoder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class OpenCliErrorEnvelopeTest {
    @Test
    void decodesCapturedYamlAndKeepsAutoFixOnlyInRawStderr() {
        String[] files = {"argument", "auth-required", "empty-result-autofix"};
        String[] codes = {"ARGUMENT", "AUTH_REQUIRED", "EMPTY_RESULT"};
        for (int i = 0; i < files.length; i++) {
            OpenCliErrorEnvelope error = OpenCliErrorDecoder.decode(fixture(files[i]), 1, false);
            assertEquals(OpenCliErrorEnvelope.Status.DECODED, error.getStatus());
            assertEquals(OpenCliErrorEnvelope.Format.YAML, error.getFormat());
            assertEquals(codes[i], error.getCode());
            assertNotNull(error.getHelp());
            assertEquals(Integer.valueOf(1), error.getObservedProcessExit());
            assertTrue(error.isExitMismatch());
        }
    }

    @Test
    void strictJsonPreservesUnknownFieldsAndTraceWithoutReadingIt() {
        String raw = "{\"ok\":false,\"error\":{\"code\":\"PLUGIN_FUTURE\",\"message\":\"failed\","
            + "\"exitCode\":77,\"help\":\"retry\",\"extension\":[false,\"off\",null]},"
            + "\"trace\":{\"summaryPath\":\"missing-fixture.json\"}}";
        OpenCliErrorEnvelope error = OpenCliErrorDecoder.decode(raw, 1, false);
        assertEquals(OpenCliErrorEnvelope.Status.DECODED, error.getStatus());
        assertEquals("PLUGIN_FUTURE", error.getCode());
        assertEquals(OpenCliErrorEnvelope.Format.JSON, error.getFormat());
        assertEquals(Integer.valueOf(77), error.getReportedExit());
        assertEquals(Arrays.asList(false, "off", null), error.getErrorFields().get("extension"));
        assertEquals("missing-fixture.json", error.getTrace().get("summaryPath"));
        assertThrows(UnsupportedOperationException.class, () -> error.getTrace().clear());
    }

    @Test
    void equivalentYamlAndJsonPreserveWireTypesAndExitProvenance() {
        String yaml = "ok: false\nerror:\n  code: PLUGIN_FUTURE\n  message: failed\n"
            + "  exitCode: 77\n  help: retry\n  values: [false, off, null, 1.25, 2147483648, 2026-11-19]\n"
            + "trace: {summaryPath: missing-fixture.json}\n# AutoFix remains in stderr\n";
        String json = "{\"ok\":false,\"error\":{\"code\":\"PLUGIN_FUTURE\",\"message\":\"failed\","
            + "\"exitCode\":77,\"help\":\"retry\",\"values\":[false,\"off\",null,1.25,2147483648,\"2026-11-19\"]},"
            + "\"trace\":{\"summaryPath\":\"missing-fixture.json\"}}";

        OpenCliErrorEnvelope yamlEnvelope = OpenCliErrorDecoder.decode(yaml, 1, false);
        OpenCliErrorEnvelope jsonEnvelope = OpenCliErrorDecoder.decode(json, 1, false);
        assertEquals(OpenCliErrorEnvelope.Status.DECODED, yamlEnvelope.getStatus());
        assertEquals(OpenCliErrorEnvelope.Status.DECODED, jsonEnvelope.getStatus());
        assertEquals(OpenCliErrorEnvelope.Format.YAML, yamlEnvelope.getFormat());
        assertEquals(OpenCliErrorEnvelope.Format.JSON, jsonEnvelope.getFormat());
        assertEquals(jsonEnvelope.getEnvelopeFields(), yamlEnvelope.getEnvelopeFields());
        assertEquals(Integer.valueOf(1), jsonEnvelope.getObservedProcessExit());
        assertEquals(jsonEnvelope.getObservedProcessExit(), yamlEnvelope.getObservedProcessExit());
        assertEquals(Integer.valueOf(77), jsonEnvelope.getReportedExit());
        assertEquals(jsonEnvelope.getReportedExit(), yamlEnvelope.getReportedExit());
        assertTrue(yamlEnvelope.isExitMismatch());
        assertTrue(jsonEnvelope.isExitMismatch());
    }

    @Test
    void rejectsUnsafeDocumentsAndDistinguishesOrdinaryDiagnostics() {
        String root = "ok: false\nerror: {code: TEST, message: failed, exitCode: 1}\n";
        for (String input : Arrays.asList(root + "value: !fixture x\n", root + "value: &fixture x\n",
                root + "value: *fixture\n", root + "---\nother: document\n", root + "ok: false\n",
                root + "value: " + repeat("[", 32) + "false" + repeat("]", 32),
                "{\"ok\":false,", "{\"ok\":false,\"ok\":false}", "{ok: false}")) {
            assertEquals(OpenCliErrorEnvelope.Status.INVALID, OpenCliErrorDecoder.decode(input, 1, false).getStatus());
        }
        assertEquals(OpenCliErrorEnvelope.Status.NO_ENVELOPE,
            OpenCliErrorDecoder.decode("warning only", 1, false).getStatus());
        assertEquals(OpenCliErrorEnvelope.Status.OVER_BUDGET,
            OpenCliErrorDecoder.decode(repeat("上", 100_000), 1, false).getStatus());
    }

    @Test
    void realSubprocessPreservesFailureAndDoesNotFollowRetrySuggestion(@TempDir Path temp) throws Exception {
        Path marker = temp.resolve("written");
        try (OpenCliClient client = new OpenCliClient(child(marker))) {
            OpenCliNonZeroExitException failure = assertThrows(OpenCliNonZeroExitException.class,
                () -> client.getExecutor().invoke("fail"));
            OpenCliResult result = failure.getPartialResult();
            assertEquals(Integer.valueOf(1), result.getExitCode());
            assertEquals(fixture("empty-result-autofix"), result.getStderr());
            assertEquals(Integer.valueOf(66), result.getErrorEnvelope().getReportedExit());
            assertTrue(result.getErrorEnvelope().isExitMismatch());
            assertEquals(Collections.singletonList("attempt"), Files.readAllLines(marker));

            OpenCliNonZeroExitException malformed = assertThrows(OpenCliNonZeroExitException.class,
                () -> client.getExecutor().invoke("malformed"));
            assertEquals(OpenCliErrorEnvelope.Status.INVALID, malformed.getPartialResult().getErrorEnvelope().getStatus());
            assertFalse(malformed.getMessage().contains("private-fixture-value"));
            assertNull(client.getExecutor().invoke("warning").getErrorEnvelope());
        }
    }

    @Test
    void malformedYamlRetainsRawStderrWithoutExposingParserDetails(@TempDir Path temp) throws Exception {
        String canary = "yaml-secret-" + UUID.randomUUID();
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        OpenCliNonZeroExitException failure;
        try (OpenCliClient client = new OpenCliClient(child(temp.resolve("unused")));
             PrintStream capture = new PrintStream(diagnostics, true, StandardCharsets.UTF_8.name())) {
            System.setErr(capture);
            try {
                failure = assertThrows(OpenCliNonZeroExitException.class,
                    () -> client.getExecutor().invoke("malformed-yaml", canary));
                failure.printStackTrace(capture);
            } finally {
                System.setErr(previousErr);
            }
        }
        OpenCliResult partial = failure.getPartialResult();
        assertEquals(OpenCliErrorEnvelope.Status.INVALID, partial.getErrorEnvelope().getStatus());
        assertEquals(OpenCliErrorEnvelope.Format.YAML, partial.getErrorEnvelope().getFormat());
        assertEquals(Integer.valueOf(1), partial.getExitCode());
        assertTrue(partial.getStderr().contains(canary));
        assertFalse(diagnostics.toString(StandardCharsets.UTF_8.name()).contains(canary));
    }

    @Test
    void typedBrowserListPreservesProcessFailureEvenWithAnEmptyArray(@TempDir Path temp) throws Exception {
        Path marker = temp.resolve("browser-commands");
        try (OpenCliClient client = new OpenCliClient(child(marker))) {
            OpenCliBrowserSession session = new OpenCliBrowserSession(client.getExecutor(), "fixture", null);
            OpenCliNonZeroExitException failure = assertThrows(OpenCliNonZeroExitException.class, session::tabListTyped);
            OpenCliResult raw = failure.getPartialResult();
            OpenCliBrowserResult<List<OpenCliBrowserTab>> decoded = OpenCliBrowserDecoder.tabList(raw);

            assertEquals("[]", raw.getStdout());
            assertEquals(Integer.valueOf(1), raw.getExitCode());
            assertEquals("BROWSER_FIXTURE", raw.getErrorEnvelope().getCode());
            assertEquals(Integer.valueOf(77), raw.getErrorEnvelope().getReportedExit());
            assertEquals("missing-fixture.json", raw.getErrorEnvelope().getTrace().get("summaryPath"));
            assertSame(raw, decoded.getRaw());
            assertEquals(OpenCliBrowserResult.Status.EXECUTION_FAILED, decoded.getStatus());
            assertNull(decoded.getValue());
        }
        assertEquals(Collections.singletonList("fixture tab list"), Files.readAllLines(marker, StandardCharsets.UTF_8));
    }

    @Test
    void incompleteTimeoutKeepsPrimaryFailure(@TempDir Path temp) {
        OpenCliProperties properties = child(temp.resolve("unused"));
        properties.setCommandTimeoutMillis(2_000);
        try (OpenCliClient client = new OpenCliClient(properties)) {
            OpenCliTimeoutException timeout = assertThrows(OpenCliTimeoutException.class,
                () -> client.getExecutor().invoke("timeout"));
            OpenCliResult partial = timeout.getPartialResult();
            assertTrue(partial.getStderr().startsWith("ok: false"));
            assertEquals(OpenCliErrorEnvelope.Status.INCOMPLETE, partial.getErrorEnvelope().getStatus());
            assertNull(partial.getErrorEnvelope().getReportedExit());
        }
    }

    private static String repeat(String value, int times) {
        return String.join("", Collections.nCopies(times, value));
    }

    private static String fixture(String name) {
        try (InputStream stream = OpenCliErrorEnvelopeTest.class.getResourceAsStream(
                "/opencli-contracts/v1/error-" + name + ".yaml");
             Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            return scanner.next();
        } catch (java.io.IOException ex) {
            throw new java.io.UncheckedIOException(ex);
        }
    }

    private static OpenCliProperties child(Path marker) {
        String binary = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        OpenCliProperties result = new OpenCliProperties();
        result.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), binary).getAbsolutePath());
        result.setLeadingArguments(Arrays.asList("-cp",
            System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            ErrorChild.class.getName(), marker.toString()));
        return result;
    }

    public static class ErrorChild {
        public static void main(String[] args) throws Exception {
            switch (args[1]) {
                case "fail":
                    Files.write(Paths.get(args[0]), Collections.singletonList("attempt"), StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                    System.err.print(fixture("empty-result-autofix"));
                    System.exit(1);
                    break;
                case "browser":
                    Files.write(Paths.get(args[0]),
                        Collections.singletonList(String.join(" ", Arrays.copyOfRange(args, 2, args.length))),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    System.out.print("[]");
                    System.err.print("ok: false\nerror: {code: BROWSER_FIXTURE, message: failed, exitCode: 77}\n"
                        + "trace: {summaryPath: missing-fixture.json}\n");
                    System.exit(1);
                    break;
                case "malformed":
                    System.err.print("{\"ok\":false,\"private-fixture-value\":");
                    System.exit(1);
                    break;
                case "malformed-yaml":
                    System.err.print("ok: false\nerror: {code: FAIL, " + args[2] + ": [\n");
                    System.exit(1);
                    break;
                case "timeout":
                    System.err.print("ok: false\nerror:");
                    Thread.sleep(30_000);
                    break;
                default:
                    System.err.print("warning only");
            }
        }
    }
}
