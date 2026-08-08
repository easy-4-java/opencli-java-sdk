package io.github.easy4j.opencli.core.support;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SubprocessExecutionSupportTest {

    @Test
    void shouldReturnDefaultMaxConcurrent() {
        int defaultVal = SubprocessExecutionSupport.defaultMaxConcurrentExecutions();
        assertTrue(defaultVal >= 2);
    }

    @Test
    void shouldConfigureMaxConcurrentExecutions() {
        SubprocessExecutionSupport.configureMaxConcurrentExecutions(8);
        // no assertion on internal state, just ensure no exception
        SubprocessExecutionSupport.configureMaxConcurrentExecutions(0); // reset to default
    }

    @Test
    void shouldHaveWaitGraceMillis() {
        assertEquals(5_000L, SubprocessExecutionSupport.WAIT_GRACE_MILLIS);
    }

    @Test
    void shouldRejectNullRequest() {
        assertThrows(NullPointerException.class, () -> SubprocessExecutionSupport.execute(null));
    }

    @Test
    void shouldExecuteSimpleCommand() throws Exception {
        SubprocessExecutionSupport.ExecutionRequest request = new SubprocessExecutionSupport.ExecutionRequest(
            org.apache.commons.exec.CommandLine.parse("echo hello"),
            null, null, 10_000L);
        SubprocessExecutionSupport.RunSession session = SubprocessExecutionSupport.execute(request);
        assertNotNull(session.getStdout());
        assertFalse(session.timedOut());
    }

    @Test
    void shouldBuildExecutionRequest() {
        org.apache.commons.exec.CommandLine cmd = org.apache.commons.exec.CommandLine.parse("echo");
        SubprocessExecutionSupport.ExecutionRequest req = new SubprocessExecutionSupport.ExecutionRequest(
            cmd, null, null, 5000L);
        assertNotNull(req.getCommandLine());
        assertNull(req.getWorkingDirectory());
        assertNull(req.getEnvironment());
        assertEquals(5000L, req.getTimeoutMillis());
    }
}
