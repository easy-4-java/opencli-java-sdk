package io.github.easy4j.opencli.browser;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.browser.support.*;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliBrowserSessionFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final OpenCliBrowserSession session = new OpenCliBrowserSession(exec, "test-sess", null);
    private final OpenCliBrowserSession bgSession = new OpenCliBrowserSession(exec, "test-sess", "background");

    @Test void shouldBind() { assertNotNull(session.bind()); }
    @Test void shouldUnbind() { assertNotNull(session.unbind()); }
    @Test void shouldTabList() { assertNotNull(session.tabList()); }
    @Test void shouldTabNew() { assertNotNull(session.tabNew()); }
    @Test void shouldTabNewUrl() { assertNotNull(session.tabNew("https://example.com")); }
    @Test void shouldTabSelect() { assertNotNull(session.tabSelect("tid", null)); }
    @Test void shouldTabClose() { assertNotNull(session.tabClose("tid", null)); }
    @Test void shouldOpen() { assertNotNull(session.open("https://example.com", null)); }
    @Test void shouldBack() { assertNotNull(session.back(null)); }
    @Test void shouldScroll() { assertNotNull(session.scroll("down", "500", null)); }
    @Test void shouldState() { assertNotNull(session.state(null, (OpenCliBrowserStateOptions) null)); }
    @Test void shouldStateDeprecated() { assertNotNull(session.state(null, (Boolean) null)); }
    @Test void shouldStateDeprecatedTrue() { assertNotNull(session.state(null, true)); }
    @Test void shouldFrames() { assertNotNull(session.frames(null)); }
    @Test void shouldScreenshot() { assertNotNull(session.screenshot(null, null, (OpenCliBrowserScreenshotOptions) null)); }
    @Test void shouldScreenshotDeprecated() { assertNotNull(session.screenshot(null, null, true, false)); }
    @Test void shouldConsole() { assertNotNull(session.console(null, (OpenCliBrowserConsoleOptions) null)); }
    @Test void shouldConsoleDeprecated() { assertNotNull(session.console(null, 10, "error")); }
    @Test void shouldAnalyze() { assertNotNull(session.analyze("https://example.com", null)); }
    @Test void shouldFind() { assertNotNull(session.find(null, null, (OpenCliBrowserFindOptions) null)); }
    @Test void shouldFindDeprecated() { assertNotNull(session.find(".btn", null, null, null, null)); }
    @Test void shouldGetTitle() { assertNotNull(session.getTitle(null)); }
    @Test void shouldGetUrl() { assertNotNull(session.getUrl(null)); }
    @Test void shouldGetText() { assertNotNull(session.getText(null, null, null, null)); }
    @Test void shouldGetValue() { assertNotNull(session.getValue(null, null, null, null)); }
    @Test void shouldGetHtml() { assertNotNull(session.getHtml(null, (OpenCliBrowserGetHtmlOptions) null)); }
    @Test void shouldGetHtmlDeprecated() { assertNotNull(session.getHtml(null, null, null, null, null, null)); }
    @Test void shouldGetAttributes() { assertNotNull(session.getAttributes(null, null, null, null)); }
    @Test void shouldClick() { assertNotNull(session.click(null, null, null, null)); }
    @Test void shouldClickNoNth() { assertNotNull(session.click(null, null, null)); }
    @Test void shouldType() { assertNotNull(session.type(null, null, null, null, null, null)); }
    @Test void shouldTypeNoNth() { assertNotNull(session.type(null, null, null, null, null)); }
    @Test void shouldHover() { assertNotNull(session.hover(null, null, null, null)); }
    @Test void shouldHoverNoNth() { assertNotNull(session.hover(null, null, null)); }
    @Test void shouldFocus() { assertNotNull(session.focus(null, null, null, null)); }
    @Test void shouldFocusNoNth() { assertNotNull(session.focus(null, null, null)); }
    @Test void shouldDblclick() { assertNotNull(session.dblclick(null, null, null, null)); }
    @Test void shouldDblclickNoNth() { assertNotNull(session.dblclick(null, null, null)); }
    @Test void shouldCheck() { assertNotNull(session.check(null, null, null, null)); }
    @Test void shouldCheckNoNth() { assertNotNull(session.check(null, null, null)); }
    @Test void shouldUncheck() { assertNotNull(session.uncheck(null, null, null, null)); }
    @Test void shouldUncheckNoNth() { assertNotNull(session.uncheck(null, null, null)); }
    @Test void shouldUpload() { assertNotNull(session.upload(null, null, null, null, null)); }
    @Test void shouldUploadNoNth() { assertNotNull(session.upload(null, null, null, null)); }
    @Test void shouldDrag() { assertNotNull(session.drag("src", "tgt", null, null)); }
    @Test void shouldFill() { assertNotNull(session.fill(null, null, null, null, null)); }
    @Test void shouldFillNoNth() { assertNotNull(session.fill(null, null, null, null)); }
    @Test void shouldSelect() { assertNotNull(session.select(null, null, null, null, null)); }
    @Test void shouldSelectNoNth() { assertNotNull(session.select(null, null, null, null)); }
    @Test void shouldKeys() { assertNotNull(session.keys("Enter", null)); }
    @Test void shouldDialogAccept() { assertNotNull(session.dialogAccept("text", null)); }
    @Test void shouldDialogDismiss() { assertNotNull(session.dialogDismiss(null)); }
    @Test void shouldWaitFor() { assertNotNull(session.waitFor("time", "1000", null, 5000L)); }
    @Test void shouldWaitForDeprecated() { assertNotNull(session.waitFor("time", "1000", null, (Integer) 5)); }
    @Test void shouldEval() { assertNotNull(session.eval("document.title", null, null)); }
    @Test void shouldExtract() { assertNotNull(session.extract(null, (OpenCliBrowserExtractOptions) null)); }
    @Test void shouldExtractDeprecated() { assertNotNull(session.extract(null, "main", 5000)); }
    @Test void shouldNetwork() { assertNotNull(session.network(null, null)); }
    @Test void shouldInit() { assertNotNull(session.init("chatgpt")); }
    @Test void shouldVerifyAdapter() { assertNotNull(session.verifyAdapter("chatgpt", null)); }
    @Test void shouldClose() { assertNotNull(session.close()); }
    @Test void shouldWorkWithBackgroundMode() { assertNotNull(bgSession.bind()); }
}
