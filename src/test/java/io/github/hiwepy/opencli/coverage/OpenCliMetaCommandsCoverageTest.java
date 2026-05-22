package io.github.hiwepy.opencli.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.meta.OpenCliMetaClient;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * OpenCLI 根级与 meta 子命令覆盖（list / validate / plugin / daemon 等）。
 * <p>
 * <strong>成功标准：</strong>SDK 门面方法完成调用并返回非 null {@link OpenCliResult}；
 * RecordingOpenCliExecutor 捕获的 argv 非空。不校验 CLI 业务结果或账号状态。
 * </p>
 * <p>由 {@code scripts/generate_opencli_command_tests.py} 生成，请勿手改。</p>
 */
class OpenCliMetaCommandsCoverageTest {

    private static void assertInvoked(RecordingOpenCliExecutor exec, OpenCliResult result) {
        assertNotNull(result);
        assertFalse(exec.lastInvocation().isEmpty());
    }

    @Test void testList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).list("json"));
        assertEquals("list", exec.lastInvocation().get(0));
    }

    @Test void testValidate() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).validate("npm/search"));
        assertEquals("validate", exec.lastInvocation().get(0));
    }

    @Test void testVerify() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).verify(null, true));
        assertEquals("verify", exec.lastInvocation().get(0));
    }

    @Test void testDoctor() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).doctor(true));
        assertEquals("doctor", exec.lastInvocation().get(0));
    }

    @Test void testConventionAudit() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).conventionAudit("twitter", null, "json", false));
        assertEquals("convention-audit", exec.lastInvocation().get(0));
    }

    @Test void testCompletion() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).completion("bash"));
        assertEquals(List.of("completion", "bash"), exec.lastInvocation());
    }

    @Test void testPluginInstall() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().install("github:user/repo"));
        assertEquals("plugin", exec.lastInvocation().get(0));
    }

    @Test void testPluginUninstall() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().uninstall("demo"));
        assertEquals(List.of("plugin", "uninstall", "demo"), exec.lastInvocation());
    }

    @Test void testPluginUpdate() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().update("demo"));
        assertEquals(List.of("plugin", "update", "demo"), exec.lastInvocation());
    }

    @Test void testPluginUpdateAll() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().updateAll());
        assertEquals(List.of("plugin", "update", "--all"), exec.lastInvocation());
    }

    @Test void testPluginList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().list("json"));
        assertEquals(List.of("plugin", "list", "-f", "json"), exec.lastInvocation());
    }

    @Test void testPluginCreate() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().create("demo", "/tmp/demo", "desc"));
        assertEquals("plugin", exec.lastInvocation().get(0));
    }

    @Test void testAdapterStatus() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).adapter().status());
        assertEquals(List.of("adapter", "status"), exec.lastInvocation());
    }

    @Test void testAdapterEject() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).adapter().eject("npm"));
        assertEquals(List.of("adapter", "eject", "npm"), exec.lastInvocation());
    }

    @Test void testAdapterReset() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).adapter().reset("npm", false));
        assertEquals(List.of("adapter", "reset", "npm"), exec.lastInvocation());
    }

    @Test void testProfileList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).profile().list());
        assertEquals(List.of("profile", "list"), exec.lastInvocation());
    }

    @Test void testProfileRename() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).profile().rename("ctx-1", "work"));
        assertEquals(List.of("profile", "rename", "ctx-1", "work"), exec.lastInvocation());
    }

    @Test void testProfileUse() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).profile().use("work"));
        assertEquals(List.of("profile", "use", "work"), exec.lastInvocation());
    }

    @Test void testDaemonStatus() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).daemon().status());
        assertEquals(List.of("daemon", "status"), exec.lastInvocation());
    }

    @Test void testDaemonStop() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).daemon().stop());
        assertEquals(List.of("daemon", "stop"), exec.lastInvocation());
    }

    @Test void testDaemonRestart() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).daemon().restart());
        assertEquals(List.of("daemon", "restart"), exec.lastInvocation());
    }

    @Test void testExternalInstall() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).external().install("gh"));
        assertEquals(List.of("external", "install", "gh"), exec.lastInvocation());
    }

    @Test void testExternalRegister() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).external().register("gh", "/usr/bin/gh", null, "GitHub CLI"));
        assertEquals("external", exec.lastInvocation().get(0));
    }

    @Test void testExternalList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).external().list("json"));
        assertEquals(List.of("external", "list", "-f", "json"), exec.lastInvocation());
    }

    @Test void testExternalPassthrough() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).external().passthrough("gh", List.of("auth", "status")));
        assertEquals(List.of("gh", "auth", "status"), exec.lastInvocation());
    }

    @Test void testAntigravityServe() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).antigravity().serve(8082, 30));
        assertEquals("antigravity", exec.lastInvocation().get(0));
    }
}
