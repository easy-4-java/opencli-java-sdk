package io.github.easy4j.opencli.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.easy4j.opencli.adapter.browser.chatgpt.ChatgptOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.claude.ClaudeOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.deepseek.DeepseekOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.gemini.GeminiOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.jimeng.JimengOpenCliClient;
import io.github.easy4j.opencli.adapter.browser.support.BrowserLlmOptions;
import io.github.easy4j.opencli.adapter.desktop.codex.CodexOpenCliClient;
import io.github.easy4j.opencli.adapter.desktop.cursor.CursorOpenCliClient;
import io.github.easy4j.opencli.adapter.desktop.support.DesktopThreadSelection;
import io.github.easy4j.opencli.adapter.publicapi.arxiv.ArxivOpenCliClient;
import io.github.easy4j.opencli.adapter.publicapi.npm.NpmDownloadPeriod;
import io.github.easy4j.opencli.adapter.publicapi.npm.NpmOpenCliClient;
import io.github.easy4j.opencli.adapter.publicapi.pypi.PypiDownloadPeriod;
import io.github.easy4j.opencli.adapter.publicapi.pypi.PypiOpenCliClient;
import io.github.easy4j.opencli.browser.OpenCliBrowserClient;
import io.github.easy4j.opencli.browser.OpenCliBrowserSession;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserConsoleOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserExtractOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserFindOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserGetHtmlOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserScreenshotOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserSemanticLocator;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserStateOptions;
import io.github.easy4j.opencli.browser.support.OpenCliBrowserTabOptions;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
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

            exec = new RecordingOpenCliExecutor();
            new NpmOpenCliClient(exec).downloads("react", "2024-01-01:2024-02-01", null);
            argv = exec.lastInvocation();
            assertEquals("downloads", argv.get(1));
            assertTrue(argv.contains("--period"));
        }

        @Test
        void npmOtherCommands() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new NpmOpenCliClient(exec).search("react", 10, true, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("search", argv.get(1));
            assertTrue(argv.contains("--limit"));
            assertTrue(argv.contains("-f"));

            exec = new RecordingOpenCliExecutor();
            new NpmOpenCliClient(exec).searchTyped("react", 10, null);
            assertTrue(exec.lastInvocation().contains("-f"));

            exec = new RecordingOpenCliExecutor();
            new NpmOpenCliClient(exec).downloadsTyped("react", NpmDownloadPeriod.LAST_WEEK, null);
            assertTrue(exec.lastInvocation().contains("-f"));

            exec = new RecordingOpenCliExecutor();
            new NpmOpenCliClient(exec).packageInfoTyped("react", null);
            assertTrue(exec.lastInvocation().contains("-f"));
        }

        @Test
        void pypiDownloadsWithPeriod() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new PypiOpenCliClient(exec).downloads("requests", PypiDownloadPeriod.RECENT, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("pypi", argv.get(0));
            assertEquals("downloads", argv.get(1));
            assertTrue(argv.contains("--period"));

            exec = new RecordingOpenCliExecutor();
            new PypiOpenCliClient(exec).downloads("requests", "last-month", null);
            assertEquals("downloads", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new PypiOpenCliClient(exec).downloadsTyped("requests", PypiDownloadPeriod.RECENT, null);
            assertTrue(exec.lastInvocation().contains("-f"));
        }

        @Test
        void pypiPackageInfo() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new PypiOpenCliClient(exec).packageInfo("requests", true, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("pypi", argv.get(0));
            assertEquals("package", argv.get(1));
            assertEquals("requests", argv.get(2));
            assertTrue(argv.contains("-f"));

            exec = new RecordingOpenCliExecutor();
            new PypiOpenCliClient(exec).packageInfoTyped("requests", null);
            assertTrue(exec.lastInvocation().contains("-f"));
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

        @Test
        void cursorLifecycleCommands() {
            CursorOpenCliClient client = new CursorOpenCliClient(new RecordingOpenCliExecutor());
            String[][] cases = {
                {"status", "status"},
                {"dump", "dump"},
                {"screenshot", "screenshot"},
                {"newTab", "new"},
                {"composer", "composer"},
                {"read", "read"},
                {"history", "history"},
                {"extractCode", "extract-code"},
                {"exportConversation", "export"}
            };
            for (String[] c : cases) {
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                CursorOpenCliClient cc = new CursorOpenCliClient(exec);
                switch (c[0]) {
                    case "status": cc.status(); break;
                    case "dump": cc.dump(); break;
                    case "screenshot": cc.screenshot(); cc.screenshot("/tmp/x.png"); break;
                    case "newTab": cc.newTab(); cc.newTab(java.util.Arrays.asList("--foo", "bar")); break;
                    case "composer": cc.composer("hi", null); break;
                    case "read": cc.read(); cc.read(null, java.util.Arrays.asList("--foo")); break;
                    case "history": cc.history(); break;
                    case "extractCode": cc.extractCode(); break;
                    case "exportConversation": cc.exportConversation(); cc.exportConversation("/tmp/out.md"); break;
                    default: break;
                }
                List<String> argv = exec.lastInvocation();
                assertEquals(c[1], argv.get(1));
            }
            client.toString(); // suppress unused
        }

        @Test
        void cursorSendAskWithNullSelection() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            CursorOpenCliClient cursor = new CursorOpenCliClient(exec);
            cursor.send("hello", null, null);
            assertEquals("send", exec.lastInvocation().get(1));
            assertFalse(exec.lastInvocation().contains("--project"));

            exec = new RecordingOpenCliExecutor();
            cursor = new CursorOpenCliClient(exec);
            cursor.ask("hi", null, null);
            assertEquals("ask", exec.lastInvocation().get(1));
        }

        @Test
        void cursorModel() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new CursorOpenCliClient(exec).model();
            assertEquals("model", exec.lastInvocation().get(1));
            assertFalse(exec.lastInvocation().contains("--model-name"));

            exec = new RecordingOpenCliExecutor();
            new CursorOpenCliClient(exec).model("claude-3.5-sonnet");
            assertEquals("claude-3.5-sonnet", exec.lastInvocation().get(2));
        }
    }

    // ------------------------------------------------------------------ newly added typed commands

    @Nested
    class NewlyAddedCommands {

        @Test
        void chatgptDeepResearchResultAndLogin() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).deepResearchResult(
                "report-id", true, 90, 6, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("deep-research-result", argv.get(1));
            assertEquals("report-id", argv.get(2));
            assertTrue(argv.contains("--wait"));
            assertTrue(argv.contains("--stable"));

            exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).login(300, null);
            argv = exec.lastInvocation();
            assertEquals("login", argv.get(1));
            assertTrue(argv.contains("--timeout"));
        }

        @Test
        void chatgptModelAndProjectCommands() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).model("gpt-4o", "p-1", null);
            List<String> argv = exec.lastInvocation();
            assertEquals("model", argv.get(1));
            assertEquals("gpt-4o", argv.get(2));
            assertTrue(argv.contains("--project"));

            exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).projectFileAdd("/tmp/a.md", "proj-7", null);
            argv = exec.lastInvocation();
            assertEquals("project-file-add", argv.get(1));
            assertEquals("/tmp/a.md", argv.get(2));
            assertTrue(argv.contains("--id"));
            assertEquals("proj-7", argv.get(argv.size() - 1));

            exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).projectList(20, null);
            argv = exec.lastInvocation();
            assertEquals("project-list", argv.get(1));
            assertTrue(argv.contains("--limit"));

            exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).whoami(null);
            argv = exec.lastInvocation();
            assertEquals("whoami", argv.get(1));
        }

        @Test
        void chatgptNewConversationAndProject() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).ask(
                "hi",
                ChatgptOpenCliClient.ChatgptCommonOptions.builder()
                    .conversation("conv-1")
                    .project("p-1")
                    .waitForResponse(true)
                    .deepResearch(true)
                    .webSearch(true)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertTrue(argv.contains("--conversation"));
            assertTrue(argv.contains("--project"));
            assertTrue(argv.contains("--wait"));
            assertTrue(argv.contains("--deep-research"));
            assertTrue(argv.contains("--web-search"));
        }

        @Test
        void chatgptDetailAndStableOptions() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).detail(
                "id-1",
                ChatgptOpenCliClient.ChatgptCommonOptions.builder()
                    .stableSeconds(3)
                    .timeoutSeconds(60)
                    .readAsMarkdown(true)
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertEquals("detail", argv.get(1));
            assertTrue(argv.contains("--stable"));
            assertTrue(argv.contains("--markdown"));

            exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).read(
                ChatgptOpenCliClient.ChatgptCommonOptions.builder().readAsMarkdown(true).build(), null);
            assertEquals("read", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--markdown"));

            exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).historyTyped(
                ChatgptOpenCliClient.ChatgptCommonOptions.builder().historyLimit(5).build(), null);
            assertTrue(exec.lastInvocation().contains("-f"));

            exec = new RecordingOpenCliExecutor();
            new ChatgptOpenCliClient(exec).askTyped("hi",
                ChatgptOpenCliClient.ChatgptCommonOptions.builder().build(), null);
            assertEquals("ask", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("-f"));
        }

        @Test
        void claudeLoginAndWhoami() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).login(120, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("claude", argv.get(0));
            assertEquals("login", argv.get(1));
            assertTrue(argv.contains("--timeout"));

            exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).whoami(null);
            argv = exec.lastInvocation();
            assertEquals("whoami", argv.get(1));
        }

        @Test
        void claudeHistoryAndRead() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).history(null);
            assertEquals("history", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).history(10, null);
            assertEquals("history", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--limit"));

            exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).read(null);
            assertEquals("read", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).newChat(null);
            assertEquals("new", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).detail("conv-1", null);
            assertEquals("detail", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).historyTyped(5, null);
            assertTrue(exec.lastInvocation().contains("-f"));

            exec = new RecordingOpenCliExecutor();
            new ClaudeOpenCliClient(exec).status(null);
            assertEquals("status", exec.lastInvocation().get(1));
        }

        @Test
        void deepseekLoginAndWhoami() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).login(60, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("deepseek", argv.get(0));
            assertEquals("login", argv.get(1));
            assertTrue(argv.contains("--timeout"));

            exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).whoami(null);
            argv = exec.lastInvocation();
            assertEquals("whoami", argv.get(1));
        }

        @Test
        void deepseekOtherCommands() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            DeepseekOpenCliClient ds = new DeepseekOpenCliClient(exec);
            ds.newChat(null);
            assertEquals("new", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).read(null);
            assertEquals("read", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).status(null);
            assertEquals("status", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).detail("conv-1", null);
            assertEquals("detail", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).history(5, null);
            assertEquals("history", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--limit"));

            exec = new RecordingOpenCliExecutor();
            new DeepseekOpenCliClient(exec).askTyped("hi",
                DeepseekOpenCliClient.DeepseekAskOptions.builder().build(), null);
            assertTrue(exec.lastInvocation().contains("-f"));
        }

        @Test
        void geminiNewCommands() {
            for (String call : new String[]{"detail", "history", "login", "models", "read", "status", "whoami"}) {
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                GeminiOpenCliClient gemini = new GeminiOpenCliClient(exec);
                switch (call) {
                    case "detail": gemini.detail("conv-1", null); break;
                    case "history": gemini.history(5, null); break;
                    case "login": gemini.login(180, null); break;
                    case "models": gemini.models(null); break;
                    case "read": gemini.read(null); break;
                    case "status": gemini.status(null); break;
                    case "whoami": gemini.whoami(null); break;
                    default: break;
                }
                List<String> argv = exec.lastInvocation();
                assertEquals("gemini", argv.get(0));
                assertEquals(call, argv.get(1));
            }
        }

        @Test
        void geminiAskModelAndThinking() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new GeminiOpenCliClient(exec).ask(
                "explain",
                BrowserLlmOptions.builder()
                    .model("gemini-2.5-pro")
                    .thinking("extended")
                    .build(),
                null);
            List<String> argv = exec.lastInvocation();
            assertTrue(argv.contains("--model"));
            assertTrue(argv.contains("gemini-2.5-pro"));
            assertTrue(argv.contains("--thinking"));
            assertTrue(argv.contains("extended"));
        }

        @Test
        void geminiDeepResearchResultAndBrowserLlmOptions() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new GeminiOpenCliClient(exec).deepResearchResult("conv-1", "exact", 30, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("deep-research-result", argv.get(1));
            assertEquals("conv-1", argv.get(2));
            assertTrue(argv.contains("--match"));
            assertTrue(argv.contains("--timeout"));

            exec = new RecordingOpenCliExecutor();
            new GeminiOpenCliClient(exec).deepResearch(
                "topic",
                BrowserLlmOptions.builder().timeoutSeconds(60).jsonOutput(true).build(),
                null);
            argv = exec.lastInvocation();
            assertEquals("deep-research", argv.get(1));
            assertTrue(argv.contains("--timeout"));
            assertTrue(argv.contains("-f"));
        }

        @Test
        void jimengLoginAndWhoami() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new JimengOpenCliClient(exec).login(120, null);
            List<String> argv = exec.lastInvocation();
            assertEquals("jimeng", argv.get(0));
            assertEquals("login", argv.get(1));
            assertTrue(argv.contains("--timeout"));

            exec = new RecordingOpenCliExecutor();
            new JimengOpenCliClient(exec).whoami(null);
            argv = exec.lastInvocation();
            assertEquals("whoami", argv.get(1));
        }

        @Test
        void jimengOtherCommands() {
            JimengOpenCliClient jimeng = new JimengOpenCliClient(new RecordingOpenCliExecutor());
            jimeng.toString(); // suppress unused

            for (String[] scenario : new String[][]{
                {"history", "history"},
                {"newWorkspace", "new"},
                {"workspaces", "workspaces"},
                {"userCredit", "user_credit"},
                {"userAssets", "user_assets"},
                {"userSubscription", "user_subscription"},
                {"generateImage2Image", "generate-image2image"},
                {"generateVideo", "generate-video"},
                {"generateImage2Video", "generate-image2video"},
                {"generateDigitalHuman", "generate-digital-human"},
                {"generateActionCopy", "generate-action-copy"}
            }) {
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                JimengOpenCliClient j = new JimengOpenCliClient(exec);
                switch (scenario[0]) {
                    case "history": j.history(5, "video", "ws", null); break;
                    case "newWorkspace": j.newWorkspace("video", null); break;
                    case "workspaces": j.workspaces(null); break;
                    case "userCredit": j.userCredit(null); break;
                    case "userAssets": j.userAssets("images", 10, null); break;
                    case "userSubscription": j.userSubscription(null); break;
                    case "generateImage2Image": j.generateImage2Image("p", "a.png,b.png", null, null); break;
                    case "generateVideo": j.generateVideo("p", null, null); break;
                    case "generateImage2Video": j.generateImage2Video("p", "a.png", null, null); break;
                    case "generateDigitalHuman": j.generateDigitalHuman("p", null, null); break;
                    case "generateActionCopy": j.generateActionCopy("/tmp/ref.png", null, null); break;
                    default: break;
                }
                assertEquals(scenario[1], exec.lastInvocation().get(1));
            }
        }

        @Test
        void codexArchiveAndSelectionCommands() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).archive(false, null, null);
            assertEquals("archive", exec.lastInvocation().get(1));
            assertFalse(exec.lastInvocation().contains("--yes"));

            exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).archive(true, null, null);
            assertTrue(exec.lastInvocation().contains("--yes"));

            exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).pin(
                DesktopThreadSelection.builder().project("p").build(), null);
            List<String> argv = exec.lastInvocation();
            assertEquals("pin", argv.get(1));
            assertTrue(argv.contains("--project"));

            exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).unpin(
                DesktopThreadSelection.builder().project("p").build(), null);
            argv = exec.lastInvocation();
            assertEquals("unpin", argv.get(1));
            assertTrue(argv.contains("--project"));

            exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).rename("new title", null, null);
            assertEquals("rename", exec.lastInvocation().get(1));
            assertEquals("new title", exec.lastInvocation().get(2));

            exec = new RecordingOpenCliExecutor();
            new CodexOpenCliClient(exec).modelList();
            argv = exec.lastInvocation();
            assertEquals("model", argv.get(1));
            assertTrue(argv.contains("--list"));
        }

        @Test
        void codexLifecycleCommands() {
            CodexOpenCliClient codex = new CodexOpenCliClient(new RecordingOpenCliExecutor());
            for (String[] scenario : new String[][]{
                {"status", "status"},
                {"dump", "dump"},
                {"screenshot", "screenshot"},
                {"extractDiff", "extract-diff"},
                {"newSession", "new"},
                {"read", "read"},
                {"projects", "projects"},
                {"history", "history"},
                {"exportConversation", "export"}
            }) {
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                CodexOpenCliClient c = new CodexOpenCliClient(exec);
                switch (scenario[0]) {
                    case "status": c.status(); break;
                    case "dump": c.dump(); break;
                    case "screenshot": c.screenshot(); c.screenshot("/tmp/x.png"); break;
                    case "extractDiff": c.extractDiff(); break;
                    case "newSession": c.newSession(); c.newSession(java.util.Arrays.asList("--foo", "bar")); break;
                    case "read": c.read(); c.read(null, java.util.Arrays.asList("--foo")); break;
                    case "projects": c.projects(); c.projects("p", 5, null); break;
                    case "history": c.history(); c.history("p", 5, null); break;
                    case "exportConversation": c.exportConversation(); c.exportConversation("/tmp/m.md"); break;
                    default: break;
                }
                List<String> argv = exec.lastInvocation();
                assertEquals(scenario[1], argv.get(1));
            }
            codex.toString(); // exercise toString coverage
        }

        @Test
        void codexSendAskModels() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            CodexOpenCliClient codex = new CodexOpenCliClient(exec);
            codex.send("hello", DesktopThreadSelection.builder().project("p").build(), null);
            List<String> argv = exec.lastInvocation();
            assertEquals("send", argv.get(1));
            assertTrue(argv.contains("--project"));

            exec = new RecordingOpenCliExecutor();
            codex = new CodexOpenCliClient(exec);
            codex.ask("what?", null, null);
            argv = exec.lastInvocation();
            assertEquals("ask", argv.get(1));

            exec = new RecordingOpenCliExecutor();
            codex = new CodexOpenCliClient(exec);
            codex.model("gpt-4o");
            argv = exec.lastInvocation();
            assertEquals("model", argv.get(1));
            assertEquals("gpt-4o", argv.get(2));
        }
    }

    @Nested
    class PublicApiFullCoverage {

        @Test
        void binancePriceAndTicker() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient(exec)
                .price("BTCUSDT", null);
            assertEquals("price", exec.lastInvocation().get(1));
            assertEquals("BTCUSDT", exec.lastInvocation().get(2));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient(exec)
                .prices(50, null);
            assertEquals("prices", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--limit"));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient(exec)
                .ticker(null);
            assertEquals("ticker", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient(exec)
                .klines("BTCUSDT", "1h", 100, null);
            assertEquals("klines", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--interval"));
            assertTrue(exec.lastInvocation().contains("--limit"));
        }

        @Test
        void binanceAllCommands() {
            io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient client =
                new io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient(new RecordingOpenCliExecutor());
            String[][] cases = {
                {"prices", "limit"},
                {"ticker", "limit"},
                {"pairs", "limit"},
                {"trades", "limit"},
                {"depth", "limit"},
                {"asks", "limit"},
                {"top", "limit"},
                {"gainers", "limit"},
                {"losers", "limit"},
            };
            for (String[] c : cases) {
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient bc =
                    new io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient(exec);
                switch (c[0]) {
                    case "prices": bc.prices(5, null); break;
                    case "ticker": bc.ticker(5, null); break;
                    case "pairs": bc.pairs(5, null); break;
                    case "trades": bc.trades("BTCUSDT", 5, null); break;
                    case "depth": bc.depth("BTCUSDT", 5, null); break;
                    case "asks": bc.asks("BTCUSDT", 5, null); break;
                    case "top": bc.top(5, true, null); break;
                    case "gainers": bc.gainers(5, null); break;
                    case "losers": bc.losers(5, null); break;
                    default: break;
                }
                List<String> argv = exec.lastInvocation();
                assertEquals(c[0], argv.get(1));
                assertTrue(argv.contains("--" + c[1]),
                    () -> "missing --" + c[1] + " in " + argv);
            }
            client.toString(); // suppress unused
        }

        @Test
        void wikipediaSearchAndPage() {
            RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(exec)
                .search("java", 10, "zh", true, null);
            assertEquals("search", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--lang"));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(exec)
                .summary("OpenCLI", "en", null);
            assertEquals("summary", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(exec)
                .page("OpenCLI", "en", 2, null);
            assertEquals("page", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--paragraphs"));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(exec)
                .trending(5, "en", null);
            assertEquals("trending", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--limit"));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(exec)
                .random("zh", null);
            assertEquals("random", exec.lastInvocation().get(1));
            assertTrue(exec.lastInvocation().contains("--lang"));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(exec)
                .random(null);
            assertEquals("random", exec.lastInvocation().get(1));

            exec = new RecordingOpenCliExecutor();
            new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(exec)
                .trending(null);
            assertEquals("trending", exec.lastInvocation().get(1));
        }

        @Test
        void wikipediaTypedAndExtensions() {
            io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient client =
                new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(
                    new RecordingOpenCliExecutor());
            for (String label : new String[]{"searchTyped", "pageTyped", "trendingTyped"}) {
                RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
                io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient c =
                    new io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient(exec);
                switch (label) {
                    case "searchTyped": c.searchTyped("java", 10, "en", null); break;
                    case "pageTyped": c.pageTyped("OpenCLI", "en", 1, null); break;
                    case "trendingTyped": c.trendingTyped(5, "en", null); break;
                    default: break;
                }
                List<String> argv = exec.lastInvocation();
                assertTrue(argv.contains("-f"));
                assertTrue(argv.contains("json"));
            }
            client.toString(); // suppress unused
        }
    }
}

