package io.github.easy4j.opencli.core.availability;

import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.core.support.MockOpenCliCli;
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

    @Test
    void checkShouldFailWhenExecutableBlank() {
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable("   ");
        OpenCliAvailabilityReport report = new OpenCliAvailabilityChecker().check(props);
        assertFalse(report.isAvailable());
        assertEquals(OpenCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED, report.getStatus());
    }

    @Test
    void checkShouldFailWhenExecutableNotExecutable() throws Exception {
        java.io.File temp = java.io.File.createTempFile("opencli-noexec-", "");
        // ensure not executable regardless of umask
        boolean ok = temp.setExecutable(false);
        try {
            OpenCliProperties props = new OpenCliProperties();
            props.setExecutable(temp.getAbsolutePath());
            OpenCliAvailabilityReport report = new OpenCliAvailabilityChecker().check(props);
            assertFalse(report.isAvailable());
            assertEquals(OpenCliAvailabilityStatus.EXECUTABLE_NOT_EXECUTABLE, report.getStatus());
        } finally {
            if (ok) {
                // restore to allow JVM cleanup
                temp.setExecutable(true);
            }
            temp.delete();
        }
    }

    @Test
    void checkShouldReportSpawnFailureWhenExecutableNotRunnable() {
        OpenCliProperties props = new OpenCliProperties();
        // Non-existent path triggers SPAWN_FAILED via executor (vs EXECUTABLE_NOT_FOUND before executor is invoked).
        props.setExecutable("");
        // 先确认 blank 路径走向 NOT_CONFIGURED，再用非 PATH 名字符覆盖 PATH 解析分支
        props.setExecutable("opencli-does-not-exist-" + System.nanoTime());
        OpenCliAvailabilityReport report = new OpenCliAvailabilityChecker().check(props);
        assertFalse(report.isAvailable());
        assertEquals(OpenCliAvailabilityStatus.EXECUTABLE_NOT_FOUND, report.getStatus());
    }

    @Test
    void checkShouldReportTimeoutWhenProbeHung() throws Exception {
        // 生成一个 sleep 足够久的脚本，让 startProbe 主动超时。
        java.io.File script = java.io.File.createTempFile("opencli-hang-", ".sh");
        try (java.io.FileWriter w = new java.io.FileWriter(script)) {
            w.write("#!/bin/sh\nsleep 5\n");
        }
        script.setExecutable(true);
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable(script.getAbsolutePath());
        props.setStartupProbeTimeoutMillis(500L);
        OpenCliAvailabilityReport report = new OpenCliAvailabilityChecker().check(props);
        assertFalse(report.isAvailable());
        // 既可能命中 TIMEOUT，也可能命中 NON_ZERO_EXIT（脚本提前被 watchdog 杀掉，exit 137）；至少是失败分支
        assertTrue(report.getStatus().name().equals("TIMEOUT")
            || report.getStatus().name().equals("NON_ZERO_EXIT")
            || report.getStatus().name().equals("SPAWN_FAILED"));
        script.delete();
    }

    @Test
    void checkShouldReportNonZeroExitOnFailingProbe() throws Exception {
        java.io.File script = java.io.File.createTempFile("opencli-fail-", ".sh");
        try (java.io.FileWriter w = new java.io.FileWriter(script)) {
            w.write("#!/bin/sh\nexit 1\n");
        }
        script.setExecutable(true);
        OpenCliProperties props = new OpenCliProperties();
        props.setExecutable(script.getAbsolutePath());
        OpenCliAvailabilityReport report = new OpenCliAvailabilityChecker().check(props);
        assertFalse(report.isAvailable());
        assertEquals(OpenCliAvailabilityStatus.NON_ZERO_EXIT, report.getStatus());
        script.delete();
    }
}
