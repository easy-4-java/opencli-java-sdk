package io.github.hiwepy.opencli.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.meta.OpenCliAuthRefreshOptions;
import io.github.hiwepy.opencli.meta.OpenCliAuthStatusOptions;
import io.github.hiwepy.opencli.meta.OpenCliExternalPassthroughOptions;
import io.github.hiwepy.opencli.meta.OpenCliMetaClient;
import io.github.hiwepy.opencli.meta.OpenCliSkillsReadOptions;
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
        assertEquals("completion", exec.lastInvocation().get(0));
        assertEquals("bash", exec.lastInvocation().get(1));
    }

    @Test void testPluginInstall() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().install("github:user/repo"));
        assertEquals("plugin", exec.lastInvocation().get(0));
    }

    @Test void testPluginUninstall() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().uninstall("demo"));
        assertEquals("plugin", exec.lastInvocation().get(0));
        assertEquals("uninstall", exec.lastInvocation().get(1));
    }

    @Test void testPluginUpdate() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().update("demo"));
        assertEquals("plugin", exec.lastInvocation().get(0));
        assertEquals("update", exec.lastInvocation().get(1));
    }

    @Test void testPluginUpdateAll() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().updateAll());
        assertEquals("plugin", exec.lastInvocation().get(0));
        assertEquals("update", exec.lastInvocation().get(1));
    }

    @Test void testPluginList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().list("json"));
        assertEquals("plugin", exec.lastInvocation().get(0));
        assertEquals("list", exec.lastInvocation().get(1));
    }

    @Test void testPluginCreate() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).plugin().create("demo", "/tmp/demo", "desc"));
        List<String> argv = exec.lastInvocation();
        assertEquals("plugin", argv.get(0));
        // 修复：dir 不再同时以 -d 和 --dir 输出，仅 --dir
        assertFalse(argv.contains("-d"), "argv should not contain short flag -d");
        int dirIdx = argv.indexOf("--dir");
        assertEquals("/tmp/demo", argv.get(dirIdx + 1));
    }

    @Test void testAdapterStatus() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).adapter().status());
        assertEquals("adapter", exec.lastInvocation().get(0));
    }

    @Test void testAdapterEject() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).adapter().eject("npm"));
        assertEquals("adapter", exec.lastInvocation().get(0));
        assertEquals("eject", exec.lastInvocation().get(1));
    }

    @Test void testAdapterReset() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).adapter().reset("npm", false));
        assertEquals("adapter", exec.lastInvocation().get(0));
        assertEquals("reset", exec.lastInvocation().get(1));
    }

    @Test void testProfileList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).profile().list());
        assertEquals("profile", exec.lastInvocation().get(0));
    }

    @Test void testProfileRename() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).profile().rename("ctx-1", "work"));
        assertEquals("profile", exec.lastInvocation().get(0));
        assertEquals("rename", exec.lastInvocation().get(1));
    }

    @Test void testProfileUse() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).profile().use("work"));
        assertEquals("profile", exec.lastInvocation().get(0));
        assertEquals("use", exec.lastInvocation().get(1));
    }

    @Test void testDaemonStatus() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).daemon().status());
        assertEquals("daemon", exec.lastInvocation().get(0));
    }

    @Test void testDaemonStop() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).daemon().stop());
        assertEquals("daemon", exec.lastInvocation().get(0));
    }

    @Test void testDaemonRestart() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).daemon().restart());
        assertEquals("daemon", exec.lastInvocation().get(0));
    }

    @Test void testExternalInstall() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).external().install("gh"));
        assertEquals("external", exec.lastInvocation().get(0));
    }

    @Test void testExternalRegister() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).external().register("gh", "/usr/bin/gh", null, "GitHub CLI"));
        assertEquals("external", exec.lastInvocation().get(0));
    }

    @Test void testExternalList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).external().list("json"));
        assertEquals("external", exec.lastInvocation().get(0));
    }

    @Test void testExternalPassthrough() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).external().passthrough(
            OpenCliExternalPassthroughOptions.builder()
                .externalCliName("gh")
                .arg("auth")
                .arg("status")
                .build()));
        assertEquals("gh", exec.lastInvocation().get(0));
        assertEquals("auth", exec.lastInvocation().get(1));
    }

    @Test void testAntigravityServe() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).antigravity().serve(8082, 30));
        assertEquals("antigravity", exec.lastInvocation().get(0));
    }

    // ---------- opencli skills (手写扩展) ----------

    @Test void testSkillsList() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).skills().list("json"));
        assertEquals("skills", exec.lastInvocation().get(0));
        assertEquals("list", exec.lastInvocation().get(1));
    }

    @Test void testSkillsRead() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).skills().read("opencli-browse", "README.md", true));
        assertEquals("skills", exec.lastInvocation().get(0));
        assertEquals("read", exec.lastInvocation().get(1));
    }

    @Test void testSkillsReadOptions() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).skills().read(
            OpenCliSkillsReadOptions.builder()
                .skill("opencli-browse")
                .asJson(true)
                .build()));
        assertEquals("skills", exec.lastInvocation().get(0));
        assertEquals("read", exec.lastInvocation().get(1));
    }

    // ---------- opencli auth (手写扩展) ----------

    @Test void testAuthStatus() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).auth().status(
            OpenCliAuthStatusOptions.builder()
                .site("npm")
                .full(true)
                .concurrency(8)
                .timeout(30)
                .format("json")
                .build()));
        assertEquals("auth", exec.lastInvocation().get(0));
        assertEquals("status", exec.lastInvocation().get(1));
    }

    @Test void testAuthRefresh() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).auth().refresh(
            OpenCliAuthRefreshOptions.builder()
                .site("npm")
                .concurrency(4)
                .timeout(60)
                .format("json")
                .build()));
        assertEquals("auth", exec.lastInvocation().get(0));
        assertEquals("refresh", exec.lastInvocation().get(1));
    }

    @Test void testAuthRefreshAll() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        assertInvoked(exec, new OpenCliMetaClient(exec).auth().refresh(
            OpenCliAuthRefreshOptions.builder().all(true).build()));
        assertEquals("auth", exec.lastInvocation().get(0));
        assertEquals("refresh", exec.lastInvocation().get(1));
    }
}
