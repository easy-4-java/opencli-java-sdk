package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.browser.OpenCliBrowserSession;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserDecoder;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserOpenResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserResult.Status;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserTab;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserSnapshot;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserSourceComparison;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserSnapshotSource;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserStateOptions;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenCliBrowserResultTest {
    @Test
    void parsesPinnedArrayAndObjectAsTheirOwnContracts() {
        OpenCliResult tabRaw = raw(fixture("tab-list"));
        OpenCliBrowserResult<List<OpenCliBrowserTab>> tabs = OpenCliBrowserDecoder.tabList(tabRaw);
        assertEquals(Status.PARSED, tabs.getStatus());
        assertSame(tabRaw, tabs.getRaw());
        assertEquals("00017", tabs.getValue().get(0).getPageId());
        assertEquals(0, tabs.getValue().get(0).getIndex());
        assertTrue(tabs.getValue().get(0).isActive());

        OpenCliBrowserResult<OpenCliBrowserOpenResult> open = OpenCliBrowserDecoder.open(raw(fixture("open")));
        assertEquals(Status.PARSED, open.getStatus());
        assertEquals("http://127.0.0.1:18765/basic.html", open.getValue().getUrl());
        assertEquals("00017", open.getValue().getPageId());
        assertFalse(open.getValue().getMetadata().has("sitemap"));

        OpenCliBrowserResult<List<OpenCliBrowserTab>> empty = OpenCliBrowserDecoder.tabList(raw(fixture("tab-list-empty")));
        assertEquals(Status.PARSED, empty.getStatus());
        assertTrue(empty.getValue().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> tabs.getValue().clear());
    }

    @Test
    void retainsLosslessIdentifiersAndUnknownMetadata() {
        OpenCliBrowserOpenResult open = OpenCliBrowserDecoder.open(raw(
            "{\"url\":\"https://example.invalid\",\"page\":9007199254740993,"
            + "\"sitemap\":{\"path\":\"/untrusted/metadata.yaml\"},\"trace\":{\"id\":\"fixture\"}}")).getValue();
        assertEquals("9007199254740993", open.getPageId());
        assertTrue(open.getMetadata().get("page").isIntegralNumber());
        assertEquals("/untrusted/metadata.yaml", open.getMetadata().get("sitemap").get("path").asText());
        assertEquals("fixture", open.getMetadata().get("trace").get("id").asText());

        OpenCliBrowserTab tab = OpenCliBrowserDecoder.tabList(raw(
            "[{\"index\":0,\"page\":\"0017\",\"active\":false,\"future\":true}]")).getValue().get(0);
        assertEquals("0017", tab.getPageId());
        assertTrue(tab.getMetadata().get("future").asBoolean());
        assertFalse(tab.isActive());
    }

    @Test
    void malformedTruncatedAndWrongLeafCannotBecomeEmptySuccess() {
        assertEquals(Status.EMPTY, OpenCliBrowserDecoder.tabList(raw("  ")).getStatus());
        assertEquals(Status.INVALID_JSON, OpenCliBrowserDecoder.tabList(raw("[{")).getStatus());
        assertEquals(Status.INVALID_JSON, OpenCliBrowserDecoder.tabList(raw("[]\u0000")).getStatus());
        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.tabList(raw("[{}]")).getStatus());
        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.open(raw("[]")).getStatus());
        OpenCliResult truncated = OpenCliResult.builder().stdout("[]").success(false)
            .executionDetails(OpenCliExecutionDetails.builder().stdoutTruncated(true).build()).build();
        assertEquals(Status.TRUNCATED, OpenCliBrowserDecoder.tabList(truncated).getStatus());
        assertNull(OpenCliBrowserDecoder.tabList(truncated).getValue());
        OpenCliResult failed = OpenCliResult.builder().stdout("[]").success(false).exitCode(2).build();
        assertEquals(Status.EXECUTION_FAILED, OpenCliBrowserDecoder.tabList(failed).getStatus());
    }

    @Test
    void typedSessionMethodsUseExistingBrowserCommandsAndKeepCloseResult() {
        OpenCliResult closeResult = raw("closed");
        OpenCliExecutor executor = new OpenCliExecutor(new OpenCliProperties()) {
            @Override
            public OpenCliResult invoke(List<String> argv) {
                assertEquals(Arrays.asList("browser", "fixture"), argv.subList(0, 2));
                List<String> command = argv.subList(2, argv.size());
                if (command.equals(Arrays.asList("tab", "list"))) {
                    return raw(fixture("tab-list"));
                }
                if (command.equals(Arrays.asList("open", "http://127.0.0.1:18765/basic.html"))) {
                    return raw(fixture("open"));
                }
                assertEquals(Arrays.asList("close"), command);
                return closeResult;
            }
        };
        OpenCliBrowserSession session = new OpenCliBrowserSession(executor, "fixture", null);
        assertEquals("00017", session.tabListTyped().getValue().get(0).getPageId());
        assertEquals("00017", session.openTyped("http://127.0.0.1:18765/basic.html", null).getValue().getPageId());
        assertSame(closeResult, session.close());
    }

    @Test
    void decodesDomAndAxTextAndPreservesSnapshotBody() {
        for (String source : Arrays.asList("dom", "ax")) {
            OpenCliResult raw = raw(fixture("state-" + source, ".stdout"));
            OpenCliBrowserResult<OpenCliBrowserSnapshot> snapshot = OpenCliBrowserDecoder.snapshot(raw);
            assertEquals(Status.PARSED, snapshot.getStatus());
            assertSame(raw, snapshot.getRaw());
            assertEquals("http://127.0.0.1:18765/basic.html", snapshot.getValue().getUrl());
            assertTrue(snapshot.getValue().getText().contains("Continue"));
            assertEquals(raw.getStdout().substring(raw.getStdout().indexOf("\n\n") + 2),
                snapshot.getValue().getText());
        }
        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.snapshot(raw("URL: fixture\n\n")).getStatus());
        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.snapshot(raw(fixture("state-compare"))).getStatus());
    }

    @Test
    void snapshotRequiresAWellFormedHeaderAndSeparator() {
        OpenCliResult windows = raw("URL: http://127.0.0.1/\r\n\r\n[1] Continue\r\n");
        OpenCliBrowserResult<OpenCliBrowserSnapshot> snapshot = OpenCliBrowserDecoder.snapshot(windows);
        assertEquals(Status.PARSED, snapshot.getStatus());
        assertEquals("http://127.0.0.1/", snapshot.getValue().getUrl());
        assertEquals("[1] Continue\r\n", snapshot.getValue().getText());

        for (String malformed : Arrays.asList(
            "URL: http://127.0.0.1/\n\r\r\n[1] Continue\n",
            "URL: http://127.0.0.1/\rbroken\n\n[1] Continue\n")) {
            OpenCliResult rawResult = raw(malformed);
            OpenCliBrowserResult<OpenCliBrowserSnapshot> decoded = OpenCliBrowserDecoder.snapshot(rawResult);
            assertEquals(Status.INVALID_SHAPE, decoded.getStatus());
            assertSame(rawResult, decoded.getRaw());
            assertNull(decoded.getValue());
        }
    }

    @Test
    void comparisonDistinguishesSourceFailureFromProcessFailure() {
        OpenCliBrowserResult<OpenCliBrowserSourceComparison> result =
            OpenCliBrowserDecoder.compareSources(raw(fixture("state-compare")));
        assertEquals(Status.PARSED, result.getStatus());
        assertEquals(77L, result.getValue().getDom().getBytes());
        assertEquals(1L, result.getValue().getAx().getInteractive());
        assertTrue(result.getValue().getAx().isOk());
        assertNull(result.getValue().getDom().getInteractive());

        OpenCliResult failedAx = raw(fixture("state-compare-ax-failure"));
        OpenCliBrowserResult<OpenCliBrowserSourceComparison> partial = OpenCliBrowserDecoder.compareSources(failedAx);
        assertEquals(Status.PARSED, partial.getStatus());
        assertTrue(partial.getValue().getDom().isOk());
        assertFalse(partial.getValue().getAx().isOk());
        assertEquals("BROWSER_AX_UNAVAILABLE", partial.getValue().getAx().getErrorCode());
        assertNull(partial.getValue().getAx().getBytes());
        assertSame(failedAx, partial.getRaw());

        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.compareSources(
            raw("{\"url\":\"\",\"sources\":{\"dom\":{\"ok\":true},\"ax\":{\"ok\":true}}}")).getStatus());
    }

    @Test
    void typedSnapshotRejectsMissingSourceAndRawPreservesFutureSource() {
        OpenCliExecutor executor = new OpenCliExecutor(new OpenCliProperties()) {
            @Override
            public OpenCliResult invoke(List<String> argv) {
                if (argv.contains("--compare-sources")) {
                    assertEquals(Arrays.asList("browser", "fixture", "state", "--compare-sources"), argv);
                    return raw(fixture("state-compare"));
                }
                assertEquals("state", argv.get(2));
                assertEquals("--source", argv.get(3));
                assertEquals(5, argv.size());
                if ("ax".equals(argv.get(4))) {
                    return raw(fixture("state-ax", ".stdout"));
                }
                assertEquals("future", argv.get(4));
                return raw("future fixture");
            }
        };
        OpenCliBrowserSession session = new OpenCliBrowserSession(executor, "fixture", null);
        assertThrows(NullPointerException.class, () -> session.snapshotTyped(null, null));
        assertEquals(Status.PARSED, session.snapshotTyped(null, OpenCliBrowserSnapshotSource.AX).getStatus());
        assertEquals(Status.PARSED, session.compareStateSourcesTyped(null).getStatus());
        assertEquals("future fixture", session.state(null,
            OpenCliBrowserStateOptions.builder().source("future").build()).getStdout());
    }

    static String fixture(String name) {
        return fixture(name, ".json");
    }

    private static String fixture(String name, String extension) {
        try (InputStream stream = OpenCliBrowserResultTest.class.getResourceAsStream(
                "/opencli-contracts/v1/browser-output-1.8.8/" + name + extension);
             Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            return scanner.next();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static OpenCliResult raw(String stdout) {
        return OpenCliResult.builder().stdout(stdout).success(true).exitCode(0).build();
    }
}
