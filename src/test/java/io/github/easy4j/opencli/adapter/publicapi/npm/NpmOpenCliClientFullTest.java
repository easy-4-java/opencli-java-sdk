package io.github.easy4j.opencli.adapter.publicapi.npm;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import org.junit.jupiter.api.Test;

class NpmOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final NpmOpenCliClient client = new NpmOpenCliClient(exec);

    @Test void shouldCallSearch() { assertNotNull(client.search("react", 10, false, null)); }
    @Test void shouldCallSearchJson() { assertNotNull(client.search("react", 10, true, null)); }
    @Test void shouldCallPackageInfo() { assertNotNull(client.packageInfo("react", false, null)); }
    @Test void shouldCallPackageInfoJson() { assertNotNull(client.packageInfo("react", true, null)); }
    @Test void shouldCallDownloadsWithPeriod() { assertNotNull(client.downloads("react", NpmDownloadPeriod.LAST_WEEK, null)); }
    @Test void shouldCallDownloadsWithNullPeriod() { assertNotNull(client.downloads("react", (NpmDownloadPeriod) null, null)); }
    @Test void shouldCallDownloadsWithRange() { assertNotNull(client.downloads("react", "2024-01-01:2024-01-31", null)); }
    @Test void shouldCallPackageInfoTyped() { assertNotNull(client.packageInfoTyped("react", null)); }
    @Test void shouldCallSearchTyped() { assertNotNull(client.searchTyped("react", 10, null)); }
    @Test void shouldCallDownloadsTyped() { assertNotNull(client.downloadsTyped("react", NpmDownloadPeriod.LAST_WEEK, null)); }
}
