package io.github.easy4j.opencli.meta;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import org.junit.jupiter.api.Test;

class OpenCliMetaSubClientsTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();

    @Test void metaList() { assertNotNull(new OpenCliMetaClient(exec).list()); }
    @Test void metaListFmt() { assertNotNull(new OpenCliMetaClient(exec).list("json")); }
    @Test void metaValidate() { assertNotNull(new OpenCliMetaClient(exec).validate(null)); }
    @Test void metaValidateTarget() { assertNotNull(new OpenCliMetaClient(exec).validate("chatgpt")); }
    @Test void metaVerify() { assertNotNull(new OpenCliMetaClient(exec).verify(null)); }
    @Test void metaVerifySmoke() { assertNotNull(new OpenCliMetaClient(exec).verify("chatgpt", true)); }
    @Test void metaDoctor() { assertNotNull(new OpenCliMetaClient(exec).doctor()); }
    @Test void metaDoctorVerbose() { assertNotNull(new OpenCliMetaClient(exec).doctor(true)); }
    @Test void metaCompletion() { assertNotNull(new OpenCliMetaClient(exec).completion("bash")); }
    @Test void metaConventionAudit() { assertNotNull(new OpenCliMetaClient(exec).conventionAudit(null, null, null, false)); }

    @Test void pluginInstall() { assertNotNull(new OpenCliPluginClient(exec).install("src")); }
    @Test void pluginUninstall() { assertNotNull(new OpenCliPluginClient(exec).uninstall("name")); }
    @Test void pluginUpdate() { assertNotNull(new OpenCliPluginClient(exec).update("name")); }
    @Test void pluginUpdateAll() { assertNotNull(new OpenCliPluginClient(exec).updateAll()); }
    @Test void pluginList() { assertNotNull(new OpenCliPluginClient(exec).list()); }
    @Test void pluginListFmt() { assertNotNull(new OpenCliPluginClient(exec).list("json")); }
    @Test void pluginCreate() { assertNotNull(new OpenCliPluginClient(exec).create("name", "/dir", "desc")); }

    @Test void adapterStatus() { assertNotNull(new OpenCliAdapterMgmtClient(exec).status()); }
    @Test void adapterEject() { assertNotNull(new OpenCliAdapterMgmtClient(exec).eject("site")); }
    @Test void adapterReset() { assertNotNull(new OpenCliAdapterMgmtClient(exec).reset("site")); }
    @Test void adapterResetAll() { assertNotNull(new OpenCliAdapterMgmtClient(exec).reset(null, true)); }

    @Test void profileList() { assertNotNull(new OpenCliProfileClient(exec).list()); }
    @Test void profileRename() { assertNotNull(new OpenCliProfileClient(exec).rename("id", "alias")); }
    @Test void profileUse() { assertNotNull(new OpenCliProfileClient(exec).use("profile")); }

    @Test void daemonStatus() { assertNotNull(new OpenCliDaemonClient(exec).status()); }
    @Test void daemonStop() { assertNotNull(new OpenCliDaemonClient(exec).stop()); }
    @Test void daemonRestart() { assertNotNull(new OpenCliDaemonClient(exec).restart()); }

    @Test void externalInstall() { assertNotNull(new OpenCliExternalClient(exec).install("name")); }
    @Test void externalRegister() { assertNotNull(new OpenCliExternalClient(exec).register("name", "bin", "cmd", "desc")); }
    @Test void externalList() { assertNotNull(new OpenCliExternalClient(exec).list()); }
    @Test void externalListFmt() { assertNotNull(new OpenCliExternalClient(exec).list("json")); }
    @Test void externalPassthrough() { assertNotNull(new OpenCliExternalClient(exec).passthrough("git", java.util.Arrays.asList("status"))); }
    @Test void externalPassthroughOpts() {
        OpenCliExternalPassthroughOptions opts = OpenCliExternalPassthroughOptions.builder().externalCliName("git").arg("status").build();
        assertNotNull(new OpenCliExternalClient(exec).passthrough(opts));
    }

    @Test void skillsList() { assertNotNull(new OpenCliSkillsClient(exec).list()); }
    @Test void skillsListFmt() { assertNotNull(new OpenCliSkillsClient(exec).list("json")); }
    @Test void skillsRead() { assertNotNull(new OpenCliSkillsClient(exec).read("skill")); }
    @Test void skillsReadJson() { assertNotNull(new OpenCliSkillsClient(exec).read("skill", true)); }
    @Test void skillsReadPath() { assertNotNull(new OpenCliSkillsClient(exec).read("skill", "path")); }
    @Test void skillsReadFull() { assertNotNull(new OpenCliSkillsClient(exec).read("skill", "path", true)); }
    @Test void skillsReadOpts() {
        OpenCliSkillsReadOptions opts = OpenCliSkillsReadOptions.builder().skill("s").path("p").asJson(true).build();
        assertNotNull(new OpenCliSkillsClient(exec).read(opts));
    }

    @Test void authStatus() { assertNotNull(new OpenCliAuthClient(exec).status()); }
    @Test void authStatusOpts() {
        OpenCliAuthStatusOptions opts = OpenCliAuthStatusOptions.builder().site("s").full(true).format("json").build();
        assertNotNull(new OpenCliAuthClient(exec).status(opts));
    }
    @Test void authRefresh() { assertNotNull(new OpenCliAuthClient(exec).refresh()); }
    @Test void authRefreshOpts() {
        OpenCliAuthRefreshOptions opts = OpenCliAuthRefreshOptions.builder().all(true).format("json").build();
        assertNotNull(new OpenCliAuthClient(exec).refresh(opts));
    }

    @Test void antigravityServe() { assertNotNull(new OpenCliAntigravityClient(exec).serve(8082, 30)); }
}
