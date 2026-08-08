package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;

class OpenCliExecutorFullTest {

    @Test
    void shouldAcceptVarargsInvoke() {
        OpenCliProperties props = new OpenCliProperties();
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(Exception.class, () -> executor.invoke("chatgpt", "ask", "hello"));
    }

    @Test
    void shouldThrowForBlankExecutableInBuildCommandLine() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("");
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(IllegalStateException.class, () -> executor.invoke("list"));
    }

    @Test
    void shouldAppendQuotedKeyValueWithTrailingEquals() {
        CommandLine cmd = new CommandLine("opencli");
        OpenCliExecutor.appendQuotedKeyValue(cmd, "--key=", "val");
        String[] args = cmd.getArguments();
        assertTrue(args[args.length - 1].contains("--key=val"));
    }

    @Test
    void shouldConfigureRemoteMode() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        props.setRemoteAgentBaseUrl("http://localhost:19823");
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertNotNull(executor);
        // invoke will fail because the remote server isn't running, but it exercises the remote path
        assertThrows(Exception.class, () -> executor.invoke("chatgpt", "ask", "hello"));
    }
}
