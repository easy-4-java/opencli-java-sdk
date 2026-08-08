package io.github.easy4j.opencli;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class OpenCliExecutionTargetTest {

    @Test
    void shouldHaveTwoValues() {
        OpenCliExecutionTarget[] values = OpenCliExecutionTarget.values();
        assertEquals(2, values.length);
    }

    @Test
    void shouldContainLocalProcess() {
        assertNotNull(OpenCliExecutionTarget.valueOf("LOCAL_PROCESS"));
    }

    @Test
    void shouldContainRemoteAgentHttp() {
        assertNotNull(OpenCliExecutionTarget.valueOf("REMOTE_AGENT_HTTP"));
    }
}
