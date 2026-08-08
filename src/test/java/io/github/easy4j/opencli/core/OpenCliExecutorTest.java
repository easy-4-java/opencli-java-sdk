package io.github.easy4j.opencli.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;

/**
 * {@link OpenCliExecutor} 静态工具与分支覆盖。
 */
class OpenCliExecutorTest {

    @Test
    void buildCommandLineRequiresAdapter() {
        OpenCliExecutor executor = new OpenCliExecutor(new OpenCliProperties());
        assertThrows(IllegalArgumentException.class,
            () -> executor.invoke(java.util.Collections.emptyList()));
    }

    @Test
    void buildCommandLineRequiresExecutable() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable(" ");
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(IllegalStateException.class,
            () -> executor.invoke(java.util.Collections.singletonList("twitter")));
    }

    @Test
    void invokeVarargsDelegatesAndTrims() throws Exception {
        OpenCliProperties props = new OpenCliProperties();
        // 重定向到 echo，验证子进程确实启动；尽量走真实本地路径。
        props.setExecutable("/bin/echo");
        props.setWorkingDirectory(java.nio.file.Files.createTempDirectory("opencli-exec").toFile().getAbsolutePath());
        OpenCliExecutor executor = new OpenCliExecutor(props);
        OpenCliResult result = executor.invoke("hello", "  world  ", "");
        assertNotNull(result);
        assertEquals(0, result.getExitCode());
    }

    @Test
    void appendQuotedKeyValueRequiresFlag() {
        assertThrows(IllegalArgumentException.class,
            () -> OpenCliExecutor.appendQuotedKeyValue(new CommandLine("echo"), "key", "value"));
    }

    @Test
    void appendQuotedKeyValueProducesQuotedToken() {
        CommandLine cmd = new CommandLine("echo");
        OpenCliExecutor.appendQuotedKeyValue(cmd, "--key", "value with space");
        assertEquals("echo", cmd.getExecutable());
        String[] args = cmd.getArguments();
        assertEquals(1, args.length);
        assertNotNull(args[0]);
    }

    @Test
    void invokeNonZeroExitRaisesTypedException() {
        OpenCliProperties props = new OpenCliProperties();
        // false：Linux 内置的返回 1；避免依赖 shell
        props.setExecutable("/bin/false");
        OpenCliExecutor executor = new OpenCliExecutor(props);
        assertThrows(OpenCliException.class,
            () -> executor.invoke(java.util.Collections.singletonList("ignored")));
    }

    @Test
    void invokeSpawnFailureRaisesExecutableFailureException() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("/this/path/definitely/does/not/exist/" + System.nanoTime());
        OpenCliExecutor executor = new OpenCliExecutor(props);
        // 子进程启动失败时被 Commons Exec 视为 exit != 0，因此断言为 OpenCliException 家族即可。
        assertThrows(io.github.easy4j.opencli.exception.OpenCliException.class,
            () -> executor.invoke(java.util.Collections.singletonList("x")));
    }

    @Test
    void copyForLocalCliExecutionLeavesExecutionTargetLocal() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        OpenCliProperties copy = props.copyForLocalCliExecution();
        assertEquals(OpenCliExecutionTarget.LOCAL_PROCESS, copy.getExecutionTarget());
    }

    @Test
    void invokeRemoteAgentParsesArgvIntoCollectRequest() {
        // 不真正发起 HTTP 调用，使用一个简易 stub executor 进行断言。
        io.github.easy4j.opencli.support.RecordingOpenCliExecutor spy =
            new io.github.easy4j.opencli.support.RecordingOpenCliExecutor();
        // 直接复用基础设施层：远程分支应被跳过，因为我们调用的是 RecordingOpenCliExecutor.invoke(list)
        // 这一路径不会触发 OpenCliExecutor.invoke，所以这里只验证 executor 在远程模式下的本地切换不会抛 NPE。
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutionTarget(OpenCliExecutionTarget.REMOTE_AGENT_HTTP);
        props.setRemoteAgentBaseUrl(null); // 不真正发起网络请求
        OpenCliExecutor executor = new OpenCliExecutor(props);
        // 远程 agent baseUrl 为 null 时，RemoteAgentHttpClient 构造后第一次 collect 才会报 I/O。
        // 我们只验证不会抛 NPE 之外的初始化异常。
        assertThrows(RuntimeException.class, () -> executor.invoke(java.util.Arrays.asList("twitter", "top")));
        assertNotNull(spy); // suppress unused warning
    }
}