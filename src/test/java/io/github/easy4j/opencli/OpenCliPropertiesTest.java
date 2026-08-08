package io.github.easy4j.opencli;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OpenCliPropertiesTest {

    @Test
    void shouldHaveDefaults() {
        OpenCliProperties p = new OpenCliProperties();
        assertEquals(OpenCliExecutionTarget.LOCAL_PROCESS, p.getExecutionTarget());
        assertEquals("opencli", p.getExecutable());
        assertNull(p.getWorkingDirectory());
        assertEquals(300_000L, p.getCommandTimeoutMillis());
        assertEquals(0, p.getMaxConcurrentExecutions());
        assertEquals(30_000L, p.getStartupProbeTimeoutMillis());
        assertNotNull(p.getLeadingArguments());
        assertTrue(p.getLeadingArguments().isEmpty());
        assertNotNull(p.getEnvironment());
        assertTrue(p.getEnvironment().isEmpty());
        assertEquals("cdp", p.getRemoteCollectMode());
        assertEquals("json", p.getRemoteOutputFormat());
        assertEquals("", p.getRemoteCdpEndpoint());
        assertFalse(p.isRemoteCaptureRawHttpResponse());
    }

    @Test
    void shouldCopyForLocalCliExecution() {
        OpenCliProperties original = new OpenCliProperties();
        original.setExecutable("/usr/bin/opencli");
        original.setWorkingDirectory("/tmp");
        original.setCommandTimeoutMillis(60_000L);
        original.setMaxConcurrentExecutions(4);
        original.setStartupProbeTimeoutMillis(10_000L);
        original.getLeadingArguments().add("--profile");
        original.getEnvironment().put("KEY", "VALUE");
        original.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        original.setRemoteAgentBaseUrl("http://example.com");

        OpenCliProperties copy = original.copyForLocalCliExecution();
        assertEquals("/usr/bin/opencli", copy.getExecutable());
        assertEquals("/tmp", copy.getWorkingDirectory());
        assertEquals(60_000L, copy.getCommandTimeoutMillis());
        assertEquals(4, copy.getMaxConcurrentExecutions());
        assertEquals(10_000L, copy.getStartupProbeTimeoutMillis());
        assertEquals(Collections.singletonList("--profile"), copy.getLeadingArguments());
        assertEquals("VALUE", copy.getEnvironment().get("KEY"));
        assertEquals(OpenCliExecutionTarget.LOCAL_PROCESS, copy.getExecutionTarget());
        assertNull(copy.getRemoteAgentBaseUrl());
    }

    @Test
    void shouldSettersAndGetters() {
        OpenCliProperties p = new OpenCliProperties();
        p.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        assertEquals(OpenCliExecutionTarget.REMOTE_AGENT_HTTP, p.getExecutionTarget());
        p.setRemoteAgentBaseUrl("http://host:8031");
        assertEquals("http://host:8031", p.getRemoteAgentBaseUrl());
        p.setRemoteCollectMode("bridge");
        assertEquals("bridge", p.getRemoteCollectMode());
        p.setRemoteOutputFormat("yaml");
        assertEquals("yaml", p.getRemoteOutputFormat());
        p.setRemoteCdpEndpoint("ws://cdp");
        assertEquals("ws://cdp", p.getRemoteCdpEndpoint());
        p.setRemoteCaptureRawHttpResponse(true);
        assertTrue(p.isRemoteCaptureRawHttpResponse());
    }
}
