package io.github.hiwepy.opencli.core.support;

import io.github.hiwepy.opencli.OpenCliProperties;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * 安装可执行的 mock {@code opencli} 脚本，用于 CLI 可用性探测单测。
 *
 * @author wandl
 * @since 1.0.0
 */
public final class MockOpenCliCli {

    private final Path scriptPath;

    private MockOpenCliCli(Path scriptPath) {
        this.scriptPath = scriptPath;
    }

    /**
     * 在临时目录安装 mock CLI。
     */
    public static MockOpenCliCli install() throws IOException {
        Path root = Files.createTempDirectory("opencli-mock-cli-");
        Path script = root.resolve("opencli");
        Files.writeString(script, buildScript(), StandardCharsets.UTF_8);
        makeExecutable(script);
        return new MockOpenCliCli(script);
    }

    /**
     * 构造绑定 mock 可执行文件的执行器。
     */
    public OpenCliExecutor newExecutor() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable(scriptPath.toAbsolutePath().toString());
        props.setCommandTimeoutMillis(5_000L);
        props.setStartupProbeTimeoutMillis(5_000L);
        return new OpenCliExecutor(props);
    }

    public Path scriptPath() {
        return scriptPath;
    }

    private static String buildScript() {
        return """
            #!/usr/bin/env bash
            set -euo pipefail
            cmd="${1:-}"
            case "$cmd" in
              list)
                echo '[]'
                exit 0
                ;;
              *)
                echo "unsupported: $*" >&2
                exit 1
                ;;
            esac
            """;
    }

    private static void makeExecutable(Path script) throws IOException {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(script, perms);
        } catch (UnsupportedOperationException ex) {
            script.toFile().setExecutable(true);
        }
    }
}
