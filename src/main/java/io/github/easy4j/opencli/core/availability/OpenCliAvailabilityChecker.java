package io.github.easy4j.opencli.core.availability;

import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.exception.OpenCliExecutableFailureException;
import io.github.easy4j.opencli.exception.OpenCliNonZeroExitException;
import io.github.easy4j.opencli.exception.OpenCliTimeoutException;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.io.File;
import java.util.Objects;
import java.util.Optional;

/**
 * 探测本机 {@code opencli} 是否已安装且可执行轻量命令 {@code opencli list}。
 *
 * @author wandl
 * @since 1.0.0
 */
public class OpenCliAvailabilityChecker {

    /**
     * 使用给定执行器与其绑定配置执行探测。
     *
     * @param executor 已构造的执行器，不得为 null
     * @return 探测报告
     */
    public OpenCliAvailabilityReport check(OpenCliExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        return check(executor.getProperties());
    }

    /**
     * 根据配置构造临时执行器并探测 CLI 可用性。
     *
     * @param properties CLI 配置，不得为 null
     * @return 探测报告
     */
    public OpenCliAvailabilityReport check(OpenCliProperties properties) {
        Objects.requireNonNull(properties, "properties");
        if (properties.getExecutionTarget() == OpenCliExecutionTarget.REMOTE_AGENT_HTTP) {
            return OpenCliAvailabilityReport.builder()
                    .status(OpenCliAvailabilityStatus.SKIPPED_REMOTE_MODE)
                    .available(true)
                    .message("execution-target=REMOTE_AGENT_HTTP, local CLI probe skipped")
                    .build();
        }
        String configured = properties.getExecutable();
        if (OpenCliStrings.isBlank(configured)) {
            return unavailable(
                    OpenCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED,
                    configured,
                    null,
                    "opencli.executable is blank",
                    null);
        }
        String trimmed = configured.trim();
        Optional<String> resolved = resolveExecutablePath(trimmed);
        if (!resolved.isPresent()) {
            if (looksLikePath(trimmed)) {
                File file = new File(trimmed);
                if (!file.exists()) {
                    return unavailable(
                            OpenCliAvailabilityStatus.EXECUTABLE_NOT_FOUND,
                            trimmed,
                            null,
                            "executable file does not exist: " + file.getAbsolutePath(),
                            null);
                }
                return unavailable(
                        OpenCliAvailabilityStatus.EXECUTABLE_NOT_EXECUTABLE,
                        trimmed,
                        file.getAbsolutePath(),
                        "executable exists but is not executable: " + file.getAbsolutePath(),
                        null);
            }
            return unavailable(
                    OpenCliAvailabilityStatus.EXECUTABLE_NOT_FOUND,
                    trimmed,
                    null,
                    "executable not found on PATH: " + trimmed,
                    null);
        }

        OpenCliProperties probeProps = copyForProbe(properties);
        OpenCliExecutor probeExecutor = new OpenCliExecutor(probeProps);
        try {
            OpenCliResult result = probeExecutor.invoke("list");
            return OpenCliAvailabilityReport.builder()
                    .status(OpenCliAvailabilityStatus.AVAILABLE)
                    .available(true)
                    .configuredExecutable(trimmed)
                    .resolvedExecutablePath(resolved.get())
                    .message("opencli list succeeded")
                    .probeResult(result)
                    .build();
        } catch (OpenCliTimeoutException ex) {
            return unavailable(
                    OpenCliAvailabilityStatus.TIMEOUT,
                    trimmed,
                    resolved.get(),
                    ex.getMessage(),
                    ex.getPartialResult());
        } catch (OpenCliNonZeroExitException ex) {
            return unavailable(
                    OpenCliAvailabilityStatus.NON_ZERO_EXIT,
                    trimmed,
                    resolved.get(),
                    ex.getMessage(),
                    ex.getPartialResult());
        } catch (OpenCliExecutableFailureException ex) {
            return unavailable(
                    OpenCliAvailabilityStatus.SPAWN_FAILED,
                    trimmed,
                    resolved.get(),
                    ex.getMessage(),
                    null);
        } catch (OpenCliException ex) {
            return unavailable(
                    OpenCliAvailabilityStatus.FAILED,
                    trimmed,
                    resolved.get(),
                    ex.getMessage(),
                    ex.getPartialResult());
        }
    }

    /**
     * 解析可执行文件：绝对/相对路径直接检查；否则在 {@code PATH} 中查找。
     */
    static Optional<String> resolveExecutablePath(String executable) {
        if (OpenCliStrings.isBlank(executable)) {
            return Optional.empty();
        }
        String trimmed = executable.trim();
        File direct = new File(trimmed);
        if (looksLikePath(trimmed)) {
            if (direct.isFile() && direct.canExecute()) {
                return Optional.of(direct.getAbsolutePath());
            }
            return Optional.empty();
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return Optional.empty();
        }
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (OpenCliStrings.isBlank(dir)) {
                continue;
            }
            File candidate = new File(dir.trim(), trimmed);
            if (candidate.isFile() && candidate.canExecute()) {
                return Optional.of(candidate.getAbsolutePath());
            }
        }
        return Optional.empty();
    }

    private static OpenCliProperties copyForProbe(OpenCliProperties source) {
        OpenCliProperties copy = source.copyForLocalCliExecution();
        long probeTimeout = source.getStartupProbeTimeoutMillis();
        if (probeTimeout <= 0) {
            probeTimeout = 30_000L;
        }
        copy.setCommandTimeoutMillis(probeTimeout);
        copy.setStartupProbeTimeoutMillis(probeTimeout);
        return copy;
    }

    private static boolean looksLikePath(String executable) {
        return executable.contains("/") || executable.contains("\\") || new File(executable).isAbsolute();
    }

    private static OpenCliAvailabilityReport unavailable(
            OpenCliAvailabilityStatus status,
            String configured,
            String resolved,
            String message,
            OpenCliResult partial) {
        return OpenCliAvailabilityReport.builder()
                .status(status)
                .available(false)
                .configuredExecutable(configured)
                .resolvedExecutablePath(resolved)
                .message(message)
                .probeResult(partial)
                .build();
    }
}
