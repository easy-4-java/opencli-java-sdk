package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import java.util.Arrays;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;

class OpenCliExecutorFullTest {

    @Test
    void shouldAcceptVarargsInvoke() {
        OpenCliProperties props = new OpenCliProperties();
        // 指向确定不存在的可执行文件，避免依赖"宿主机恰好没装 opencli"。
        props.setExecutable("/nonexistent/opencli-for-test");
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
    void shouldTruncateOversizedOutputWithMarker() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("/bin/sh");
        props.setMaxOutputBytes(64L);
        props.setCommandTimeoutMillis(30_000L);
        OpenCliExecutor executor = new OpenCliExecutor(props);

        OpenCliResult result = executor.invoke("-c",
            "printf 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'");

        assertTrue(result.isSuccess(), "truncation must not fail the run");
        assertTrue(result.getStdout().contains("truncated"),
            "overflow must leave a truncation marker: " + result.getStdout());
    }

    @Test
    void shouldDecodeUtf8OutputRegardlessOfPlatformCharset() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("/bin/sh");
        props.setCommandTimeoutMillis(30_000L);
        OpenCliExecutor executor = new OpenCliExecutor(props);

        // 你 = \344\275\240, 好 = \345\245\275（POSIX printf 八进制转义）。
        // Java 源码里反斜杠必须双写，否则编译器会把 \344 当成八进制转义吃掉。
        OpenCliResult result = executor.invoke(Arrays.asList(
            "-c", "printf '\\344\\275\\240\\345\\245\\275'"));

        assertTrue(result.isSuccess(), "stderr=" + result.getStderr());
        assertEquals("你好", result.getStdout().trim(),
            "UTF-8 bytes must decode with explicit UTF-8, not the platform charset");
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
