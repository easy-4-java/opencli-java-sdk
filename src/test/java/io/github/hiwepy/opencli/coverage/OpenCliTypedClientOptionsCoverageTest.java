package io.github.hiwepy.opencli.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hiwepy.opencli.adapter.browser.chatgpt.ChatgptOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.claude.ClaudeOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.deepseek.DeepseekOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.gemini.GeminiOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.jimeng.JimengOpenCliClient;
import io.github.hiwepy.opencli.adapter.browser.support.BrowserLlmOptions;
import io.github.hiwepy.opencli.adapter.desktop.codex.CodexOpenCliClient;
import io.github.hiwepy.opencli.adapter.desktop.cursor.CursorOpenCliClient;
import io.github.hiwepy.opencli.adapter.desktop.support.DesktopThreadSelection;
import io.github.hiwepy.opencli.adapter.publicapi.arxiv.ArxivOpenCliClient;
import io.github.hiwepy.opencli.adapter.publicapi.npm.NpmDownloadPeriod;
import io.github.hiwepy.opencli.adapter.publicapi.npm.NpmOpenCliClient;
import io.github.hiwepy.opencli.adapter.publicapi.pypi.PypiDownloadPeriod;
import io.github.hiwepy.opencli.adapter.publicapi.pypi.PypiOpenCliClient;
import io.github.hiwepy.opencli.browser.OpenCliBrowserClient;
import io.github.hiwepy.opencli.browser.OpenCliBrowserSession;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserConsoleOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserExtractOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserFindOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserGetHtmlOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserScreenshotOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserSemanticLocator;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserStateOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserTabOptions;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 强类型 SDK 门面 + Options 构建器的 argv 覆盖（canonical path）。
 * <p>
 * 与 {@link OpenCliAdapterCommandsCoverageTest} 的分工：
 * manifest 层对长尾 adapter 子命令做 {@link io.github.hiwepy.opencli.core.OpenCliAdapterCommandRequest} 冒烟；
 * 本类对已有 typed client / Options 的入口断言 Options→argv 映射。
 * browser 细粒度回归另见 {@code OpenCliBrowserArgvTest}。
 * </p>
 */
class OpenCliTypedClientOptionsCoverageTest {

    // ------------------------------------------------------------------ browser

    @Nested
    class BrowserSessionOptions {

        private RecordingOpenCliExecutor exec;
        private OpenCliBrowserSession session;

        @BeforeEach
        void setUp() {
            exec = new RecordingOpenCliExecutor();
            session = new OpenCliBrowserClient(exec).session("typed-opt", "background");
        }

        private List<String> argv() {
            return exec.lastInvocation();
        }

        private void assertBrowserInvoked(OpenCliResult result) {
            assertNotNull(result);
            assertFalse(argv().isEmpty());
            assertEquals("browser", argv().get(0));
        }

        @Test
        void tabOptionsOnOpen() {
            assertBrowserInvoked(session.open(
                "https://example.com",
                OpenCliBrowserTabOptions.builder().tab("tab-1").build()));
            assertTrue(argv().contains("--tab"));
            assertTrue(argv().contains("tab-1"));
        }

        @Test
        void extractOptionsChunkSizeAndStart() {
            assertBrowserInvoked(session.extract(
                OpenCliBrowserTabOptions.builder().tab("t1").build(),
                OpenCliBrowserExtractOptions.builder()
                    .selector("main")
                    .chunkSize(20000)
                    .start(40000)
                    .build()));
            assertTrue(argv().contains("--chunk-size"));
            assertTrue(argv().contains("20000"));
            assertTrue(argv().contains("--start"));
            assertTrue(argv().contains("40000"));
        }

        @Test
        void findOptionsCssLimitTextMax() {
            assertBrowserInvoked(session.find(
                OpenCliBrowserSemanticLocator.builder().build(),
                OpenCliBrowserTabOptions.builder().tab("t1").build(),
                OpenCliBrowserFindOptions.builder().css(".btn").limit(10).textMax(80).build()));
            assertTrue(argv().contains("--css"));
            assertTrue(argv().contains("--limit"));
            assertTrue(argv().contains("--text-max"));
        }

        @Test
        void stateOptionsSourceAndCompareSources() {
            assertBrowserInvoked(session.state(
                OpenCliBrowserTabOptions.builder().tab("t1").build(),
                OpenCliBrowserStateOptions.builder().source("ax").compareSources(true).build()));
            assertTrue(argv().contains("--source"));
            assertTrue(argv().contains("ax"));
            assertTrue(argv().contains("--compare-sources"));
        }

        @Test
        void screenshotOptionsWidthHeightFullPage() {
            assertBrowserInvoked(session.screenshot(
                "/tmp/s.png",
                OpenCliBrowserTabOptions.builder().tab("t1").build(),
                OpenCliBrowserScreenshotOptions.builder()
                    .width(1280)
                    .height(720)
                    .fullPage(true)
                    .build()));
            assertTrue(argv().contains("--width"));
            assertTrue(argv().contains("1280"));
            assertTrue(argv().contains("--height"));
            assertTrue(argv().contains("720"));
        }

        @Test
        void consoleOptionsLevelSinceUntilFollow() {
            assertBrowserInvoked(session.console(
                OpenCliBrowserTabOptions.builder().tab("t1").build(),
                OpenCliBrowserConsoleOptions.builder()
                    .level("error")
                    .since("30s")
                    .until("2m")
                    .follow(true)
                    .build()));
            assertTrue(argv().contains("--level"));
            assertTrue(argv().contains("--since"));
            assertTrue(argv().contains("--until"));
            assertTrue(argv().contains("--follow"));
        }

        @Test
        void getHtmlOptionsSelectorAndBudgets() {
            assertBrowserInvoked(session.getHtml(
                OpenCliBrowserTabOptions.builder().tab("t1").build(),
                OpenCliBrowserGetHtmlOptions.builder()
                    .selector("#app")
                    .as("json")
                    .max(5000)
                    .depth(2)
                    .childrenMax(10)
                    .textMax(120)
                    .build()));
            assertTrue(argv().contains("--selector"));
            assertTrue(argv().contains("--children-max"));
            assertTrue(argv().contains("--text-max"));
        }

        @Test
        void networkOptions() {
            assertBrowserInvoked(session.network(
                OpenCliBrowserSession.OpenCliBrowserNetworkOptions.builder()
                    .filterFields("xhr")
                    .follow(true)
                    .build(),
                OpenCliBrowserTabOptions.builder().tab("t1").build()));
            assertTrue(argv().contains("--filter"));
            assertTrue(argv().contains("xhr"));
            assertTrue(argv().contains("--follow"));
        }

        @Test
        void verifyOptions() {
            assertBrowserInvoked(session.verifyAdapter(
                "twitter/me",
                OpenCliBrowserSession.OpenCliBrowserVerifyOptions.builder()
                    .writeFixture(true)
                    .trace("on")
                    .build()));
            assertTrue(argv().contains("--write-fixture"));
            assertTrue(argv().contains("--trace"));
            assertTrue(argv().contains("on"));
        }
    }

    // ------------------------------------------------------------------ browser LLM adapters

    @Nested
    class ClaudeOptions {

        @Test
        void askOptionsModelThinkFileJson() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).ask(
                "hello",
                ClaudeOpenCliClient.ClaudeAskOptions.builder()
                    .newConversation(true)
                    .timeoutSeconds(120)
                    .model("opus")
                    .adaptiveThinking(true)
                    .attachmentPath("/tmp/doc.pdf")
                    .jsonOutput(true)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("claude", argv.get(0));
            assertEquals("ask", argv.get(1));
            assertTrue(argv.contains("--new"));
            assertTrue(argv.contains("--timeout"));
            assertTrue(argv.contains("120"));
            assertTrue(argv.contains("--model"));
            assertTrue(argv.contains("opus"));
            assertTrue(argv.contains("--think"));
            assertTrue(argv.contains("--file"));
            assertTrue(argv.contains("-f"));
            assertTrue(argv.contains("json"));
        }

        @Test
        void sendReusesAskOptions() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).send(
                "follow-up",
                ClaudeOpenCliClient.ClaudeAskOptions.builder().newConversation(true).build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("send", argv.get(1));
            assertTrue(argv.contains("--new"));
        }
    }

    @Nested
    class GeminiOptions {

        @Test
        void askBrowserLlmOptions() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new GeminiOpenCliClient(exec).ask(
                "prompt",
                BrowserLlmOptions.builder()
                    .timeoutSeconds(90)
                    .startNewChat(true)
                    .siteSession("ephemeral")
                    .jsonOutput(true)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("gemini", argv.get(0));
            assertTrue(argv.contains("--timeout"));
            assertTrue(argv.contains("--new"));
            assertTrue(argv.contains("--site-session"));
            assertTrue(argv.contains("ephemeral"));
            assertTrue(argv.contains("-f"));
        }

        @Test
        void deepResearchOptionsToolConfirmJson() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new GeminiOpenCliClient(exec).deepResearch(
                "topic",
                GeminiOpenCliClient.GeminiDeepResearchOptions.builder()
                    .timeoutSeconds(300)
                    .tool("web")
                    .confirmLabel("Start research")
                    .jsonOutput(true)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("deep-research", argv.get(1));
            assertTrue(argv.contains("--tool"));
            assertTrue(argv.contains("web"));
            assertTrue(argv.contains("--confirm"));
            assertTrue(argv.contains("Start research"));
            assertTrue(argv.contains("-f"));
        }

        @Test
        void imageArgsAspectStyleOutputSkipDownload() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new GeminiOpenCliClient(exec).image(
                "a cat",
                GeminiOpenCliClient.GeminiImageArgs.builder()
                    .aspectRatio("16:9")
                    .style("photo")
                    .outputDir("/tmp/out")
                    .skipDownload(true)
                    .timeoutSeconds(60)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("image", argv.get(1));
            assertTrue(argv.contains("--rt"));
            assertTrue(argv.contains("16:9"));
            assertTrue(argv.contains("--st"));
            assertTrue(argv.contains("--op"));
            assertTrue(argv.contains("--sd"));
        }
    }

    @Nested
    class ChatgptOptions {

        @Test
        void commonOptionsAsk() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).ask(
                "hi",
                ChatgptOpenCliClient.ChatgptCommonOptions.builder()
                    .newConversation(true)
                    .timeoutSeconds(45)
                    .historyLimit(5)
                    .readAsMarkdown(true)
                    .jsonOutput(true)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("chatgpt", argv.get(0));
            assertEquals("ask", argv.get(1));
            assertTrue(argv.contains("--new"));
            assertTrue(argv.contains("--limit"));
            assertTrue(argv.contains("--markdown"));
            assertTrue(argv.contains("-f"));
        }

        @Test
        void imageOptionsReferenceOutputSkipDownload() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).image(
                "draw",
                ChatgptOpenCliClient.ChatgptImageOptions.builder()
                    .referenceImagePath("/tmp/ref.png")
                    .outputDir("/tmp/out")
                    .skipDownload(true)
                    .timeoutSeconds(120)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("image", argv.get(1));
            assertTrue(argv.contains("--image"));
            assertTrue(argv.contains("/tmp/ref.png"));
            assertTrue(argv.contains("--op"));
            assertTrue(argv.contains("--sd"));
            assertTrue(argv.contains("--timeout"));
        }

        @Test
        void historyCommonOptions() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).history(
                ChatgptOpenCliClient.ChatgptCommonOptions.builder().historyLimit(10).build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("history", argv.get(1));
            assertTrue(argv.contains("--limit"));
            assertTrue(argv.contains("10"));
        }
    }

    @Nested
    class DeepseekOptions {

        @Test
        void askOptionsModelThinkSearchFile() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).ask(
                "q",
                DeepseekOpenCliClient.DeepseekAskOptions.builder()
                    .newConversation(true)
                    .timeoutSeconds(60)
                    .model("expert")
                    .deepThink(true)
                    .webSearch(true)
                    .attachmentPath("/tmp/x.txt")
                    .jsonOutput(true)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("deepseek", argv.get(0));
            assertTrue(argv.contains("--new"));
            assertTrue(argv.contains("--model"));
            assertTrue(argv.contains("expert"));
            assertTrue(argv.contains("--think"));
            assertTrue(argv.contains("--search"));
            assertTrue(argv.contains("--file"));
            assertTrue(argv.contains("-f"));
        }

        @Test
        void sendOptions() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).send(
                "conv-1",
                "msg",
                DeepseekOpenCliClient.DeepseekAskOptions.builder().timeoutSeconds(30).build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("send", argv.get(1));
            assertEquals("conv-1", argv.get(2));
            assertTrue(argv.contains("--timeout"));
        }
    }

    @Nested
    class JimengOptions {

        @Test
        void generateOptionsModelWaitWorkspaceTone() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new JimengOpenCliClient(exec).generate(
                "prompt",
                JimengOpenCliClient.JimengGenerateOptions.builder()
                    .model("v3")
                    .waitSeconds(10)
                    .timeoutSeconds(120)
                    .workspace("ws-1")
                    .jsonOutput(true)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("jimeng", argv.get(0));
            assertEquals("generate", argv.get(1));
            assertTrue(argv.contains("--model"));
            assertTrue(argv.contains("--wait=10"));
            assertTrue(argv.contains("--workspace=ws-1"));
            assertTrue(argv.contains("-f"));
        }

        @Test
        void generateAudioToneOptions() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new JimengOpenCliClient(exec).generateAudio(
                "script",
                JimengOpenCliClient.JimengGenerateOptions.builder()
                    .tone("warm")
                    .toneFile("/tmp/tone.wav")
                    .cloneFile("/tmp/clone.wav")
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("generate-audio", argv.get(1));
            assertTrue(argv.contains("--tone"));
            assertTrue(argv.contains("warm"));
            assertTrue(argv.contains("--tone-file"));
            assertTrue(argv.contains("--clone-file"));
        }
    }

    // ------------------------------------------------------------------ public API adapters

    @Nested
    class PublicApiClients {

        @Test
        void npmPackageInfoJson() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new NpmOpenCliClient(exec).packageInfo("react", true, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("npm", argv.get(0));
            assertEquals("package", argv.get(1));
            assertEquals("react", argv.get(2));
            assertTrue(argv.contains("-f"));
            assertTrue(argv.contains("json"));
        }

        @Test
        void npmDownloadsWithPeriod() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new NpmOpenCliClient(exec).downloads("react", NpmDownloadPeriod.LAST_MONTH, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("downloads", argv.get(1));
            assertTrue(argv.contains("--period"));
        }

        @Test
        void pypiDownloadsWithPeriod() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new PypiOpenCliClient(exec).downloads("requests", PypiDownloadPeriod.RECENT, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("pypi", argv.get(0));
            assertEquals("downloads", argv.get(1));
            assertTrue(argv.contains("--period"));
        }

        @Test
        void arxivSearchWithLimit() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ArxivOpenCliClient(exec).search("transformer", 5, true, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("arxiv", argv.get(0));
            assertEquals("search", argv.get(1));
            assertTrue(argv.contains("--limit"));
            assertTrue(argv.contains("5"));
        }
    }

    // ------------------------------------------------------------------ desktop adapters

    @Nested
    class DesktopClients {

        private DesktopThreadSelection sampleSelection() {
            return DesktopThreadSelection.builder()
                .project("demo")
                .conversation("main")
                .conversationIndex(1)
                .threadId("local:abc")
                .timeoutSeconds(30)
                .build();
        }

        @Test
        void codexModelReadOmitsPositional() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).model();
            List<String> argv = exec.lastInvocation();
            assertEquals("codex", argv.get(0));
            assertEquals("model", argv.get(1));
            assertFalse(argv.contains("--model-name"));
        }

        @Test
        void codexModelSwitchUsesPositional() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).model("gpt-4");
            List<String> argv = exec.lastInvocation();
            assertEquals("codex", argv.get(0));
            assertEquals("model", argv.get(1));
            assertEquals("gpt-4", argv.get(2));
        }

        @Test
        void cursorModelSwitchUsesPositional() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new CursorOpenCliClient(exec).model("claude-3.5-sonnet");
            List<String> argv = exec.lastInvocation();
            assertEquals("cursor", argv.get(0));
            assertEquals("model", argv.get(1));
            assertEquals("claude-3.5-sonnet", argv.get(2));
        }

        @Test
        void codexAskWithDesktopThreadSelection() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).ask("hello", sampleSelection(), null);
            List<String> argv = exec.lastInvocation();
            assertEquals("ask", argv.get(1));
            assertTrue(argv.contains("--project"));
            assertTrue(argv.contains("demo"));
            assertTrue(argv.contains("--thread-id"));
            assertTrue(argv.contains("local:abc"));
        }

        @Test
        void cursorSendWithDesktopThreadSelection() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new CursorOpenCliClient(exec).send("msg", sampleSelection(), null);
            List<String> argv = exec.lastInvocation();
            assertEquals("cursor", argv.get(0));
            assertEquals("send", argv.get(1));
            assertTrue(argv.contains("--conversation"));
            assertTrue(argv.contains("main"));
        }
    }
}
