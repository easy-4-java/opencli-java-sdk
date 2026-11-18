package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.browser.OpenCliBrowserSession;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserAnalysisResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserConsoleResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserFindResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserNetworkResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserOpenResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserResult.Status;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserSnapshot;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserSourceComparison;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserTab;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserFindOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserTabOptions;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserSnapshotSource;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Opt-in verification against an externally provisioned disposable Browser Bridge. */
class OpenCliBrowserBridgeIT {
    @Test
    void typedResultsFromAnIsolatedChromePage() {
        String fixtureUrl = fixtureUrl();
        try (OpenCliClient client = new OpenCliClient(properties())) {
            OpenCliBrowserSession session = new OpenCliBrowserSession(client.getExecutor(), "sdk-bridge-fixture", "background");
            OpenCliBrowserOpenResult opened = parsed(session.openTyped(fixtureUrl, null));
            assertEquals(fixtureUrl, opened.getUrl());
            assertNotNull(opened.getPageId());

            List<OpenCliBrowserTab> tabs = parsed(session.tabListTyped());
            OpenCliBrowserTab openedTab = tabs.stream()
                .filter(tab -> opened.getPageId().equals(tab.getPageId()) && fixtureUrl.equals(tab.getUrl()))
                .findFirst().orElseThrow(() -> new AssertionError("The opened page must appear in the tab list"));
            assertEquals(openedTab.getMetadata().get("active").asBoolean(), openedTab.isActive());

            OpenCliBrowserSnapshot dom = parsed(session.snapshotTyped(null, OpenCliBrowserSnapshotSource.DOM));
            assertEquals(fixtureUrl, dom.getUrl());
            assertTrue(dom.getText().contains("Continue"));

            OpenCliBrowserSnapshot ax = parsed(session.snapshotTyped(null, OpenCliBrowserSnapshotSource.AX));
            assertEquals(fixtureUrl, ax.getUrl());
            assertTrue(ax.getText().contains("RootWebArea"));
            assertTrue(ax.getText().contains("Bridge fixture"));

            OpenCliBrowserSourceComparison comparison = parsed(session.compareStateSourcesTyped(null));
            assertEquals(fixtureUrl, comparison.getUrl());
            assertTrue(comparison.getDom().isOk());
            assertTrue(comparison.getAx().isOk());
        }
    }

    @Test
    void queryAndCaptureUseTheRealBridgeResponses() {
        String fixtureUrl = fixtureUrl();
        try (OpenCliClient client = new OpenCliClient(properties())) {
            OpenCliBrowserSession session = new OpenCliBrowserSession(client.getExecutor(), "sdk-capture-fixture", "background");
            parsed(session.openTyped(fixtureUrl, null));

            OpenCliBrowserFindOptions options = OpenCliBrowserFindOptions.builder().css("#go").build();
            OpenCliBrowserFindResult found = parsed(session.findTyped(null, null, options));
            assertEquals(1, found.getMatchesN());
            assertEquals("Continue", found.getEntries().get(0).getText());
            assertNotNull(found.getEntries().get(0).getRef());

            OpenCliBrowserConsoleResult console = parsed(session.consoleTyped(null, null));
            assertEquals("sdk-capture-fixture", console.getSession());
            assertTrue(console.getMessages().isEmpty());

            String endpoint = URI.create(fixtureUrl).resolve("/api/items").toString();
            OpenCliBrowserNetworkResult network = parsed(session.networkTyped(null, null));
            assertTrue(network.getEntries().stream().anyMatch(entry ->
                endpoint.equals(entry.getUrl()) && "GET".equals(entry.getMethod()) && entry.getStatus() == 200));

            OpenCliBrowserAnalysisResult analysis = parsed(session.analyzeTyped(fixtureUrl, null));
            assertEquals(fixtureUrl, analysis.getFinalUrl());
            assertEquals("Bridge fixture", analysis.getTitle());
        }
    }

    @Test
    void boundTabSurvivesTypedSuccessFailureAndClientShutdown() {
        String url = URI.create(fixtureUrl()).resolve("/bound-fixture.html").toString();
        String pageId;
        try (OpenCliClient client = new OpenCliClient(properties())) {
            OpenCliBrowserSession session = new OpenCliBrowserSession(client.getExecutor(), "sdk-bound-fixture", null);
            session.bind();
            pageId = boundPage(session, url).getPageId();
            assertEquals(url, parsed(session.snapshotTyped(null, OpenCliBrowserSnapshotSource.DOM)).getUrl());

            OpenCliBrowserTabOptions missing = OpenCliBrowserTabOptions.builder().tab("missing-fixture-page").build();
            OpenCliNonZeroExitException failure = assertThrows(OpenCliNonZeroExitException.class,
                () -> session.snapshotTyped(missing, OpenCliBrowserSnapshotSource.DOM));
            assertNotNull(failure.getPartialResult());
            assertEquals(pageId, boundPage(session, url).getPageId());
        }
        try (OpenCliClient client = new OpenCliClient(properties())) {
            OpenCliBrowserSession session = new OpenCliBrowserSession(client.getExecutor(), "sdk-bound-fixture", null);
            assertEquals(pageId, boundPage(session, url).getPageId());
            session.unbind();
        }
    }

    private static OpenCliBrowserTab boundPage(OpenCliBrowserSession session, String url) {
        return parsed(session.tabListTyped()).stream()
            .filter(tab -> url.equals(tab.getUrl())).findFirst()
            .orElseThrow(() -> new AssertionError("The original bound tab must remain open"));
    }

    private static OpenCliProperties properties() {
        OpenCliProperties properties = new OpenCliProperties();
        properties.setExecutable(requiredEnvironment("OPENCLI_INSTALLED_NODE"));
        properties.setLeadingArguments(Collections.singletonList(requiredEnvironment("OPENCLI_INSTALLED_ENTRY")));
        properties.setCommandTimeoutMillis(30_000L);
        return properties;
    }

    private static String fixtureUrl() {
        String url = requiredEnvironment("OPENCLI_BRIDGE_FIXTURE_URL");
        URI uri = URI.create(url);
        assertEquals("http", uri.getScheme());
        assertEquals("127.0.0.1", uri.getHost());
        return url;
    }

    private static String requiredEnvironment(String name) {
        return Objects.requireNonNull(System.getenv(name), name + " fixture is required");
    }

    private static <T> T parsed(OpenCliBrowserResult<T> result) {
        assertEquals(Status.PARSED, result.getStatus());
        assertNotNull(result.getValue());
        return result.getValue();
    }
}
