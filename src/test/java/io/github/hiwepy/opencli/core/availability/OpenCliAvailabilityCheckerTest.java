package io.github.hiwepy.opencli.core.availability;

import io.github.hiwepy.opencli.OpenCliExecutionTarget;
import io.github.hiwepy.opencli.OpenCliProperties;
import io.github.hiwepy.opencli.core.support.MockOpenCliCli;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OpenCliAvailabilityChecker} 单元测试。
 */
class OpenCliAvailabilityCheckerTest {

    @Test
    void checkShouldSucceedWithMockExecutable() throws Exception {
        MockOpenCliCli mock = MockOpenCliCli.install();
        OpenCliAvailabilityReport report = new OpenCliAvailabilityChecker().check(mock.newExecutor());

        assertTrue(report.isAvailable());
        assertEquals(OpenCliAvailabilityStatus.AVAILABLE, report.getStatus());
    }

    @Test
    void checkShouldSkipWhenRemoteMode() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        props.setRemoteAgentBaseUrl("http://127.0.0.1:19823");

        OpenCliAvailabilityReport report = new OpenCliAvailabilityChecker().check(props);

        assertTrue(report.isAvailable());
        assertEquals(OpenCliAvailabilityStatus.SKIPPED_REMOTE_MODE, report.getStatus());
    }

    @Test
    void checkShouldFailWhenExecutableMissing() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("/nonexistent/opencli-startup-test");
        props.setStartupProbeTimeoutMillis(3_000L);

        OpenCliAvailabilityReport report = new OpenCliAvailabilityChecker().check(props);

        assertFalse(report.isAvailable());
        assertEquals(OpenCliAvailabilityStatus.EXECUTABLE_NOT_FOUND, report.getStatus());
    }
}
