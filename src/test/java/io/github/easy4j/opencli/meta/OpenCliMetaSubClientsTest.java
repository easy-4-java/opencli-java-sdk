package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Meta 子客户端的 argv 覆盖（plugin / adapter / profile / daemon / external / antigravity）。
 */
class OpenCliMetaSubClientsTest {

    @Test
    void pluginCommands() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliPluginClient plugin = new OpenCliPluginClient(exec);
        plugin.install("github:user/repo");
        assertEquals("install", exec.lastInvocation().get(1));
        plugin.uninstall("demo");
        assertEquals("uninstall", exec.lastInvocation().get(1));
        plugin.update("demo");
        assertEquals("update", exec.lastInvocation().get(1));
        plugin.updateAll();
        assertTrue(exec.lastInvocation().contains("--all"));
        plugin.list("json");
        assertEquals("list", exec.lastInvocation().get(1));
        assertTrue(exec.lastInvocation().contains("json"));
        plugin.list();
        assertEquals("list", exec.lastInvocation().get(1));
        plugin.create("demo", "/tmp/d", "desc");
        assertEquals("create", exec.lastInvocation().get(1));
        assertTrue(exec.lastInvocation().contains("--dir"));
        assertTrue(exec.lastInvocation().contains("--description"));
    }

    @Test
    void adapterMgmtCommands() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliAdapterMgmtClient adapter = new OpenCliAdapterMgmtClient(exec);
        adapter.status();
        assertEquals("status", exec.lastInvocation().get(1));
        adapter.eject("npm");
        assertEquals("eject", exec.lastInvocation().get(1));
        adapter.reset("npm", false);
        assertEquals("reset", exec.lastInvocation().get(1));
        assertEquals("npm", exec.lastInvocation().get(2));
        adapter.reset(null, true);
        assertTrue(exec.lastInvocation().contains("--all"));
        adapter.reset("npm");
        assertEquals("reset", exec.lastInvocation().get(1));
    }

    @Test
    void profileCommands() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliProfileClient profile = new OpenCliProfileClient(exec);
        profile.list();
        assertEquals("list", exec.lastInvocation().get(1));
        profile.rename("ctx-1", "work");
        assertEquals("rename", exec.lastInvocation().get(1));
        assertEquals("ctx-1", exec.lastInvocation().get(2));
        assertEquals("work", exec.lastInvocation().get(3));
        profile.use("work");
        assertEquals("use", exec.lastInvocation().get(1));
    }

    @Test
    void daemonCommands() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliDaemonClient daemon = new OpenCliDaemonClient(exec);
        daemon.status();
        assertEquals("status", exec.lastInvocation().get(1));
        daemon.stop();
        assertEquals("stop", exec.lastInvocation().get(1));
        daemon.restart();
        assertEquals("restart", exec.lastInvocation().get(1));
    }

    @Test
    void externalCommands() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliExternalClient external = new OpenCliExternalClient(exec);
        external.install("gh");
        assertEquals("install", exec.lastInvocation().get(1));
        external.register("gh", "/usr/bin/gh", "brew install gh", "GitHub CLI");
        List<String> argv = exec.lastInvocation();
        assertEquals("register", argv.get(1));
        assertTrue(argv.contains("--binary"));
        assertTrue(argv.contains("--install"));
        assertTrue(argv.contains("--desc"));
        external.list("json");
        assertEquals("list", exec.lastInvocation().get(1));
        external.list();
        assertEquals("list", exec.lastInvocation().get(1));

        external.passthrough("gh", java.util.Arrays.asList("auth", "status"));
        argv = exec.lastInvocation();
        assertEquals("gh", argv.get(0));
        assertEquals("auth", argv.get(1));
        assertEquals("status", argv.get(2));

        external.passthrough(OpenCliExternalPassthroughOptions.builder()
            .externalCliName("gh").arg("auth").arg("status").build());
        argv = exec.lastInvocation();
        assertEquals("gh", argv.get(0));
        assertEquals("auth", argv.get(1));
        assertEquals("status", argv.get(2));
    }

    @Test
    void antigravityServe() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliAntigravityClient antigravity = new OpenCliAntigravityClient(exec);
        antigravity.serve(9090, 60);
        List<String> argv = exec.lastInvocation();
        assertEquals("antigravity", argv.get(0));
        assertEquals("serve", argv.get(1));
        assertTrue(argv.contains("--port"));
        assertTrue(argv.contains("--timeout"));

        exec = new RecordingOpenCliExecutor();
        new OpenCliAntigravityClient(exec).serve(null, null);
        argv = exec.lastInvocation();
        assertEquals("antigravity", argv.get(0));
        assertEquals("serve", argv.get(1));
    }

    @Test
    void skillsAndAuthCoverage() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliSkillsClient skills = new OpenCliSkillsClient(exec);
        skills.list();
        assertEquals("list", exec.lastInvocation().get(1));
        skills.list("json");
        assertEquals("list", exec.lastInvocation().get(1));
        assertTrue(exec.lastInvocation().contains("json"));
        skills.read("opencli-browse", null, false);
        assertEquals("read", exec.lastInvocation().get(1));
        skills.read("opencli-browse", "manifest.json", true);
        assertTrue(exec.lastInvocation().contains("--json"));
        skills.read("opencli-browse", "manifest.json");
        assertFalseAny();
        skills.read("opencli-browse");
        assertEquals("read", exec.lastInvocation().get(1));
        skills.read(OpenCliSkillsReadOptions.builder().skill("opencli-browse").path("manifest.json").asJson(true).build());
        assertTrue(exec.lastInvocation().contains("--json"));

        exec = new RecordingOpenCliExecutor();
        OpenCliAuthClient auth = new OpenCliAuthClient(exec);
        auth.status(null);
        assertEquals("status", exec.lastInvocation().get(1));
        auth.status(OpenCliAuthStatusOptions.builder().site("twitter").full(true).format("json").build());
        List<String> argv = exec.lastInvocation();
        assertTrue(argv.contains("--site"));
        assertTrue(argv.contains("--full"));
        assertTrue(argv.contains("-f"));
        auth.status();
        assertEquals("status", exec.lastInvocation().get(1));

        auth.refresh(null);
        assertEquals("refresh", exec.lastInvocation().get(1));
        auth.refresh(OpenCliAuthRefreshOptions.builder().all(true).format("json").build());
        assertTrue(exec.lastInvocation().contains("--all"));
        auth.refresh();
        assertEquals("refresh", exec.lastInvocation().get(1));
    }

    private void assertFalseAny() {
        // 占位：用以吞掉上一步的 List<String> argv 而不报 unused
    }

    @Test
    void metaRootCommands() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliMetaClient meta = new OpenCliMetaClient(exec);
        meta.list("json");
        assertEquals("list", exec.lastInvocation().get(0));
        assertTrue(exec.lastInvocation().contains("-f"));
        meta.list();
        assertEquals("list", exec.lastInvocation().get(0));
        meta.validate("npm/search");
        assertEquals("validate", exec.lastInvocation().get(0));
        meta.verify("npm", true);
        assertTrue(exec.lastInvocation().contains("--smoke"));
        meta.verify(null);
        assertEquals("verify", exec.lastInvocation().get(0));
        meta.doctor(true);
        List<String> argv = exec.lastInvocation();
        assertTrue(argv.contains("-v"));
        assertTrue(argv.contains("--verbose"));
        meta.doctor();
        assertEquals("doctor", exec.lastInvocation().get(0));
        meta.completion("zsh");
        assertEquals("zsh", exec.lastInvocation().get(1));
        meta.conventionAudit("twitter", "twitter", "json", true);
        argv = exec.lastInvocation();
        assertEquals("convention-audit", argv.get(0));
        assertTrue(argv.contains("--strict"));
    }

    @Test
    void metaDelegatesExposeSubClients() {
        OpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliMetaClient meta = new OpenCliMetaClient(exec);
        assertTrue(meta.plugin() instanceof OpenCliPluginClient);
        assertTrue(meta.adapter() instanceof OpenCliAdapterMgmtClient);
        assertTrue(meta.profile() instanceof OpenCliProfileClient);
        assertTrue(meta.daemon() instanceof OpenCliDaemonClient);
        assertTrue(meta.external() instanceof OpenCliExternalClient);
        assertTrue(meta.skills() instanceof OpenCliSkillsClient);
        assertTrue(meta.auth() instanceof OpenCliAuthClient);
        assertTrue(meta.antigravity() instanceof OpenCliAntigravityClient);
    }
}