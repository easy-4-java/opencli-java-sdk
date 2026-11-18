package io.github.easy4j.opencli.contract;

import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.browser.OpenCliBrowserSession;
import io.github.easy4j.opencli.browser.OpenCliBrowserSession.OpenCliBrowserNetworkOptions;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserAnalysisResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserConsoleResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserDecoder;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserFindResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserNetworkResult;
import io.github.easy4j.opencli.browser.result.OpenCliBrowserResult.Status;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserConsoleOptions;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenCliBrowserEvidenceResultTest {
    @Test
    void findsLimitedAndLosslessMatches() {
        OpenCliBrowserFindResult found = OpenCliBrowserDecoder.find(raw("find")).getValue();
        assertEquals(2L, found.getMatchesN());
        assertEquals(1, found.getEntries().size());
        assertEquals("Continue", found.getEntries().get(0).getText());
        assertEquals(BigInteger.valueOf(17), found.getEntries().get(0).getRef());
        String large = raw("find").getStdout().replace("\"ref\": 17", "\"ref\": 9007199254740993");
        assertEquals(new BigInteger("9007199254740993"), OpenCliBrowserDecoder.find(success(large))
            .getValue().getEntries().get(0).getRef());
        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.find(success("{\"matches_n\":0,\"entries\":[{}]}")).getStatus());
    }

    @Test
    void decodesConsoleAndNetworkCaptureEnvelopes() {
        OpenCliBrowserConsoleResult console = OpenCliBrowserDecoder.console(raw("console")).getValue();
        assertEquals("fixture", console.getSession());
        assertEquals("2025-01-01T00:00:00.000Z", console.getCapturedAt());
        assertEquals("Fixture warning", console.getMessages().get(0).getText());
        assertEquals("warning", console.getMessages().get(0).getType());
        assertEquals(Status.PARSED, OpenCliBrowserDecoder.console(success(
            "{\"session\":\"s\",\"captured_at\":\"2025-01-01T00:00:00Z\",\"count\":0,\"messages\":[]}")).getStatus());
        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.console(success(
            "{\"session\":\"s\",\"captured_at\":\"2025-01-01T00:00:00Z\",\"count\":1,\"messages\":[]}")).getStatus());

        OpenCliBrowserNetworkResult network = OpenCliBrowserDecoder.network(raw("network")).getValue();
        assertEquals(0L, network.getFilteredOut());
        assertEquals("GET 127.0.0.1:18765/api/items", network.getEntries().get(0).getKey());
        assertEquals(200, network.getEntries().get(0).getStatus());
        assertTrue(network.getEntries().get(0).getMetadata().get("shape").isObject());
        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.network(raw("console")).getStatus());
        OpenCliResult failed = OpenCliResult.builder().stdout(raw("network").getStdout()).exitCode(1).build();
        assertSame(failed, OpenCliBrowserDecoder.network(failed).getRaw());
        assertEquals(Status.EXECUTION_FAILED, OpenCliBrowserDecoder.network(failed).getStatus());
    }

    @Test
    void retainsAnalysisMetadataWithoutFollowingAdvice() {
        OpenCliBrowserAnalysisResult analysis = OpenCliBrowserDecoder.analyze(raw("analyze")).getValue();
        assertEquals("Fixture page", analysis.getTitle());
        assertEquals("A", analysis.getPattern());
        assertEquals("http://127.0.0.1:18765/basic.html", analysis.getFinalUrl());
        assertEquals(1, analysis.getMetadata().get("api_candidates").size());
        assertTrue(analysis.getMetadata().get("recommended_next_step").isTextual());
        assertFalse(analysis.getMetadata().has("sitemap"));
        assertEquals(Status.INVALID_SHAPE, OpenCliBrowserDecoder.analyze(raw("network")).getStatus());
    }

    @Test
    void typedEntrypointsReuseExistingArgvAndRejectStreamingOrDetailBeforeLaunch() {
        OpenCliExecutor executor = new OpenCliExecutor(new OpenCliProperties()) {
            @Override
            public OpenCliResult invoke(List<String> argv) {
                assertEquals("browser", argv.get(0));
                assertEquals("fixture", argv.get(1));
                assertFalse(argv.contains("--follow"));
                assertFalse(argv.contains("--detail"));
                return raw(argv.get(2));
            }
        };
        OpenCliBrowserSession session = new OpenCliBrowserSession(executor, "fixture", null);
        assertEquals(Status.PARSED, session.findTyped(null, null, null).getStatus());
        assertEquals(Status.PARSED, session.consoleTyped(null, null).getStatus());
        assertEquals(Status.PARSED, session.networkTyped(null, null).getStatus());
        assertEquals(Status.PARSED, session.analyzeTyped("http://127.0.0.1:18765/basic.html", null).getStatus());
        assertThrows(IllegalArgumentException.class,
            () -> session.consoleTyped(null, OpenCliBrowserConsoleOptions.builder().follow(true).build()));
        assertThrows(IllegalArgumentException.class,
            () -> session.networkTyped(OpenCliBrowserNetworkOptions.builder().follow(true).build(), null));
        assertThrows(IllegalArgumentException.class,
            () -> session.networkTyped(OpenCliBrowserNetworkOptions.builder().detailKey("key").build(), null));
    }

    private static OpenCliResult raw(String name) {
        return success(OpenCliBrowserResultTest.fixture(name));
    }

    private static OpenCliResult success(String stdout) {
        return OpenCliResult.builder().stdout(stdout).success(true).exitCode(0).build();
    }
}
