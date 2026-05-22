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
    @Test void testBrowserTabNew() { assertBrowserInvoked(session.tabNew("https://example.com")); }
    @Test void testBrowserTabSelect() { assertBrowserInvoked(session.tabSelect("t1", null)); }
    @Test void testBrowserTabClose() { assertBrowserInvoked(session.tabClose("t1", null)); }
    @Test void testBrowserOpen() { assertBrowserInvoked(session.open("https://example.com", null)); }
    @Test void testBrowserBack() { assertBrowserInvoked(session.back(null)); }
    @Test void testBrowserScroll() { assertBrowserInvoked(session.scroll("down", "500", null)); }
    @Test void testBrowserState() {
        assertBrowserInvoked(session.state(null, OpenCliBrowserStateOptions.builder().build()));
    }
    @Test void testBrowserFrames() { assertBrowserInvoked(session.frames(null)); }
    @Test void testBrowserScreenshot() {
        assertBrowserInvoked(session.screenshot("/tmp/cov.png", null, OpenCliBrowserScreenshotOptions.builder().build()));
    }
    @Test void testBrowserConsole() {
        assertBrowserInvoked(session.console(null, OpenCliBrowserConsoleOptions.builder().build()));
    }
    @Test void testBrowserAnalyze() { assertBrowserInvoked(session.analyze("https://example.com", null)); }
    @Test void testBrowserFind() {
        assertBrowserInvoked(session.find(
            null, null, OpenCliBrowserFindOptions.builder().css(".x").build()));
    }
    @Test void testBrowserGetTitle() { assertBrowserInvoked(session.getTitle(null)); }
    @Test void testBrowserGetUrl() { assertBrowserInvoked(session.getUrl(null)); }
    @Test void testBrowserGetText() {
        assertBrowserInvoked(session.getText("1", OpenCliBrowserSemanticLocator.builder().role("button").build(), null, null));
    }
    @Test void testBrowserGetValue() {
        assertBrowserInvoked(session.getValue("1", OpenCliBrowserSemanticLocator.builder().role("textbox").build(), null, null));
    }
    @Test void testBrowserGetHtml() {
        assertBrowserInvoked(session.getHtml(null, OpenCliBrowserGetHtmlOptions.builder().build()));
    }
    @Test void testBrowserGetAttributes() {
        assertBrowserInvoked(session.getAttributes("1", OpenCliBrowserSemanticLocator.builder().role("button").build(), null, null));
    }
    @Test void testBrowserClick() {
        assertBrowserInvoked(session.click("1", OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserType() {
        assertBrowserInvoked(session.type("1", "hello", OpenCliBrowserSemanticLocator.builder().build(), null, false));
    }
    @Test void testBrowserHover() {
        assertBrowserInvoked(session.hover("1", OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserFocus() {
        assertBrowserInvoked(session.focus("1", OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserDblclick() {
        assertBrowserInvoked(session.dblclick("1", OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserCheck() {
        assertBrowserInvoked(session.check("1", OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserUncheck() {
        assertBrowserInvoked(session.uncheck("1", OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserUpload() {
        assertBrowserInvoked(session.upload("/tmp/a.txt", java.util.List.of("/tmp/a.txt"),
            OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserDrag() {
        assertBrowserInvoked(session.drag("src", "dst",
            OpenCliBrowserDragLocator.builder().fromRole("button").toRole("button").build(), null));
    }
    @Test void testBrowserFill() {
        assertBrowserInvoked(session.fill("1", "x", OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserSelect() {
        assertBrowserInvoked(session.select("1", "opt", OpenCliBrowserSemanticLocator.builder().build(), null));
    }
    @Test void testBrowserKeys() { assertBrowserInvoked(session.keys("Enter", null)); }
    @Test void testBrowserDialogAccept() { assertBrowserInvoked(session.dialogAccept(null, null)); }
    @Test void testBrowserDialogDismiss() { assertBrowserInvoked(session.dialogDismiss(null)); }
    @Test void testBrowserWait() { assertBrowserInvoked(session.waitFor("selector", ".loaded", null, 1000L)); }
    @Test void testBrowserEval() { assertBrowserInvoked(session.eval("1+1", null, null)); }
    @Test void testBrowserExtract() {
        assertBrowserInvoked(session.extract(null, OpenCliBrowserExtractOptions.builder().selector("main").build()));
    }
    @Test void testBrowserNetwork() {
        assertBrowserInvoked(session.network(OpenCliBrowserSession.OpenCliBrowserNetworkOptions.builder().build(), null));
    }
    @Test void testBrowserInit() { assertBrowserInvoked(session.init("twitter/me")); }
    @Test void testBrowserVerify() {
        assertBrowserInvoked(session.verifyAdapter("twitter/me",
            OpenCliBrowserSession.OpenCliBrowserVerifyOptions.builder().build()));
    }
    @Test void testBrowserClose() { assertBrowserInvoked(session.close()); }
}
