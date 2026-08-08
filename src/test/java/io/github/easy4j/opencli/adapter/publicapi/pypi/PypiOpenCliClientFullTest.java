package io.github.easy4j.opencli.adapter.publicapi.pypi;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import org.junit.jupiter.api.Test;

class PypiOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final PypiOpenCliClient client = new PypiOpenCliClient(exec);

    @Test void shouldCallPackageInfo() { assertNotNull(client.packageInfo("requests", false, null)); }
    @Test void shouldCallPackageInfoJson() { assertNotNull(client.packageInfo("requests", true, null)); }
    @Test void shouldCallDownloadsWithPeriod() { assertNotNull(client.downloads("requests", PypiDownloadPeriod.RECENT, null)); }
    @Test void shouldCallDownloadsWithNullPeriod() { assertNotNull(client.downloads("requests", (PypiDownloadPeriod) null, null)); }
    @Test void shouldCallDownloadsWithString() { assertNotNull(client.downloads("requests", "recent", null)); }
    @Test void shouldCallDownloadsWithNullString() { assertNotNull(client.downloads("requests", (String) null, null)); }
    @Test void shouldCallPackageInfoTyped() { assertNotNull(client.packageInfoTyped("requests", null)); }
    @Test void shouldCallDownloadsTyped() { assertNotNull(client.downloadsTyped("requests", PypiDownloadPeriod.RECENT, null)); }
    @Test void shouldCallDownloadsTypedNullPeriod() { assertNotNull(client.downloadsTyped("requests", null, null)); }
}
