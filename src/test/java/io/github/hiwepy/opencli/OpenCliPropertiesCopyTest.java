package io.github.hiwepy.opencli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * {@link OpenCliProperties#copyForLocalCliExecution()} 行为测试。
 */
class OpenCliPropertiesCopyTest {

    @Test
    void copyForcesLocalAndCopiesExecutableEnv() {
        OpenCliProperties p = new OpenCliProperties();
        p.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        p.setExecutable("/usr/bin/opencli");
        p.getEnvironment().put("OPENCLI_CDP_ENDPOINT", "http://127.0.0.1:9222");
        p.setLeadingArguments(java.util.List.of("--verbose"));

        OpenCliProperties c = p.copyForLocalCliExecution();
        assertSame(OpenCliExecutionTarget.LOCAL_PROCESS, c.getExecutionTarget());
        assertEquals("/usr/bin/opencli", c.getExecutable());
        assertEquals("http://127.0.0.1:9222", c.getEnvironment().get("OPENCLI_CDP_ENDPOINT"));
        assertEquals(1, c.getLeadingArguments().size());
        // 原对象不变
        assertSame(OpenCliExecutionTarget.REMOTE_AGENT_HTTP, p.getExecutionTarget());
    }
}
