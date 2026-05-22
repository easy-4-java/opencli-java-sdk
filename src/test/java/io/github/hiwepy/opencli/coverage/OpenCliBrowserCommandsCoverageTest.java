package io.github.hiwepy.opencli.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.hiwepy.opencli.browser.OpenCliBrowserClient;
import io.github.hiwepy.opencli.browser.OpenCliBrowserSession;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserConsoleOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserDragLocator;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserExtractOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserFindOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserGetHtmlOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserScreenshotOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserSemanticLocator;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserStateOptions;
import io.github.hiwepy.opencli.browser.support.OpenCliBrowserTabOptions;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code opencli browser} 全部叶子子命令覆盖（与 cli.ts 对齐，共 42 条）。
 * <p>
 * <strong>成功标准：</strong>{@link OpenCliBrowserSession} 方法返回非 null {@link OpenCliResult}，
 * argv 以 {@code browser} 开头。不校验页面内容或浏览器实例是否可用。
 * </p>
 * <p>由 {@code scripts/generate_opencli_command_tests.py} 生成，请勿手改。</p>
 */
class OpenCliBrowserCommandsCoverageTest {

    private static final String SESSION = "cov-session";
    private static final OpenCliBrowserTabOptions TAB =
        OpenCliBrowserTabOptions.builder().tab("t1").build();

    private RecordingOpenCliExecutor exec;
    private OpenCliBrowserSession session;

    @BeforeEach
    void setUp() {
        exec = new RecordingOpenCliExecutor();
        session = new OpenCliBrowserClient(exec).session(SESSION, "background");
    }

    private void assertBrowserInvoked(OpenCliResult result) {
        assertNotNull(result);
        assertFalse(exec.lastInvocation().isEmpty());
        assertEquals("browser", exec.lastInvocation().get(0));
    }

    @Test void testBrowserBind() { assertBrowserInvoked(session.bind()); }
    @Test void testBrowserUnbind() { assertBrowserInvoked(session.unbind()); }
    @Test void testBrowserTabList() { assertBrowserInvoked(session.tabList()); }
    @Test void testBrowserTabNew() {
        assertBrowserInvoked(session.tabNew("https://example.com"));
    }
    @Test void testBrowserTabSelect() { assertBrowserInvoked(session.tabSelect("t1", TAB)); }
    @Test void testBrowserTabClose() { assertBrowserInvoked(session.tabClose("t1", TAB)); }
    @Test void testBrowserOpen() {
        assertBrowserInvoked(session.open("https://example.com", TAB));
    }
    @Test void testBrowserBack() { assertBrowserInvoked(session.back(TAB)); }
    @Test void testBrowserScroll() {
        assertBrowserInvoked(session.scroll("down", "500", TAB));
    }
    @Test void testBrowserState() {
        assertBrowserInvoked(session.state(TAB, OpenCliBrowserStateOptions.builder()
            .source("ax")
            .compareSources(true)
            .build()));
    }
    @Test void testBrowserFrames() { assertBrowserInvoked(session.frames(TAB)); }
    @Test void testBrowserScreenshot() {
        assertBrowserInvoked(session.screenshot("/tmp/cov.png", TAB,
            OpenCliBrowserScreenshotOptions.builder().width(1280).height(720).fullPage(true).build()));
    }
    @Test void testBrowserConsole() {
        assertBrowserInvoked(session.console(TAB, OpenCliBrowserConsoleOptions.builder()
            .level("error")
            .since("30s")
            .until("2m")
            .follow(true)
            .build()));
    }
    @Test void testBrowserAnalyze() {
        assertBrowserInvoked(session.analyze("https://example.com", TAB));
    }
    @Test void testBrowserFind() {
        assertBrowserInvoked(session.find(
            OpenCliBrowserSemanticLocator.builder().build(),
            TAB,
            OpenCliBrowserFindOptions.builder().css(".x").limit(10).textMax(80).build()));
    }
    @Test void testBrowserGetTitle() { assertBrowserInvoked(session.getTitle(TAB)); }
    @Test void testBrowserGetUrl() { assertBrowserInvoked(session.getUrl(TAB)); }
    @Test void testBrowserGetText() {
        assertBrowserInvoked(session.getText("1",
            OpenCliBrowserSemanticLocator.builder().role("button").build(), TAB, null));
    }
    @Test void testBrowserGetValue() {
        assertBrowserInvoked(session.getValue("1",
            OpenCliBrowserSemanticLocator.builder().role("textbox").build(), TAB, null));
    }
    @Test void testBrowserGetHtml() {
        assertBrowserInvoked(session.getHtml(TAB, OpenCliBrowserGetHtmlOptions.builder()
            .selector("#app")
            .as("json")
            .max(5000)
            .depth(2)
            .childrenMax(10)
            .textMax(120)
            .build()));
    }
    @Test void testBrowserGetAttributes() {
        assertBrowserInvoked(session.getAttributes("1",
            OpenCliBrowserSemanticLocator.builder().role("button").build(), TAB, null));
    }
    @Test void testBrowserClick() {
        assertBrowserInvoked(session.click("1",
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserType() {
        assertBrowserInvoked(session.type("1", "hello",
            OpenCliBrowserSemanticLocator.builder().build(), TAB, false));
    }
    @Test void testBrowserHover() {
        assertBrowserInvoked(session.hover("1",
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserFocus() {
        assertBrowserInvoked(session.focus("1",
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserDblclick() {
        assertBrowserInvoked(session.dblclick("1",
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserCheck() {
        assertBrowserInvoked(session.check("1",
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserUncheck() {
        assertBrowserInvoked(session.uncheck("1",
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserUpload() {
        assertBrowserInvoked(session.upload("/tmp/a.txt",
            java.util.List.of("/tmp/a.txt"),
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserDrag() {
        assertBrowserInvoked(session.drag("src", "dst",
            OpenCliBrowserDragLocator.builder().fromRole("button").toRole("button").build(), TAB));
    }
    @Test void testBrowserFill() {
        assertBrowserInvoked(session.fill("1", "x",
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserSelect() {
        assertBrowserInvoked(session.select("1", "opt",
            OpenCliBrowserSemanticLocator.builder().build(), TAB));
    }
    @Test void testBrowserKeys() { assertBrowserInvoked(session.keys("Enter", TAB)); }
    @Test void testBrowserDialogAccept() { assertBrowserInvoked(session.dialogAccept(null, TAB)); }
    @Test void testBrowserDialogDismiss() { assertBrowserInvoked(session.dialogDismiss(TAB)); }
    @Test void testBrowserWait() {
        assertBrowserInvoked(session.waitFor("selector", ".loaded", TAB, 1000L));
    }
    @Test void testBrowserEval() { assertBrowserInvoked(session.eval("1+1", TAB, null)); }
    @Test void testBrowserExtract() {
        assertBrowserInvoked(session.extract(TAB, OpenCliBrowserExtractOptions.builder()
            .selector("main")
            .chunkSize(20000)
            .start(40000)
            .build()));
    }
    @Test void testBrowserNetwork() {
        assertBrowserInvoked(session.network(
            OpenCliBrowserSession.OpenCliBrowserNetworkOptions.builder()
                .filterFields("xhr")
                .follow(true)
                .build(),
            TAB));
    }
    @Test void testBrowserInit() { assertBrowserInvoked(session.init("twitter/me")); }
    @Test void testBrowserVerify() {
        assertBrowserInvoked(session.verifyAdapter("twitter/me",
            OpenCliBrowserSession.OpenCliBrowserVerifyOptions.builder()
                .writeFixture(true)
                .trace("on")
                .build()));
    }
    @Test void testBrowserClose() { assertBrowserInvoked(session.close()); }
}
