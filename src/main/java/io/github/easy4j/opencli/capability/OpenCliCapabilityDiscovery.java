package io.github.easy4j.opencli.capability;

import io.github.easy4j.opencli.OpenCliExecutionTarget;
import io.github.easy4j.opencli.OpenCliProperties;
import io.github.easy4j.opencli.capability.OpenCliCapabilitySnapshot.Status;
import io.github.easy4j.opencli.core.OpenCliExecutionDetails.TerminationReason;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.exception.OpenCliException;
import io.github.easy4j.opencli.exception.OpenCliTimeoutException;
import io.github.easy4j.opencli.parser.OpenCliHelpParser;
import io.github.easy4j.opencli.parser.OpenCliHelpResult;
import io.github.easy4j.opencli.util.OpenCliStrings;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fixed, bounded metadata probes through the existing local subprocess runtime. */
public final class OpenCliCapabilityDiscovery {
    private static final int MAX_PROBE_OUTPUT_BYTES = 1024 * 1024;
    private static final long MAX_PROBE_MILLIS = 10_000;
    private static final long MAX_CLEANUP_MILLIS = 1_000;
    private static final Pattern VERSION = Pattern.compile(
        "[ \t\r\n]*([0-9]+\\.[0-9]+\\.[0-9]+(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?)[ \t\r\n]*");

    private final OpenCliExecutor executor;
    private long nextAttempt;
    private long lastPublishedAttempt;
    private final Map<OpenCliHelpScope, OpenCliCapabilitySnapshot> lastAvailable =
        new EnumMap<>(OpenCliHelpScope.class);

    /** @param executor client executor whose local process capacity is shared */
    public OpenCliCapabilityDiscovery(OpenCliExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Observes version and root help on a configured, trusted absolute executable.
     * Installation, launcher arguments and environment are supplied by the trusted caller.
     *
     * @param executor client executor whose local process capacity is shared
     * @return the root observation or an explicit reason it could not be made
     */
    public static OpenCliCapabilitySnapshot discover(OpenCliExecutor executor) {
        return new OpenCliCapabilityDiscovery(executor).refresh();
    }

    /**
     * Runs a fresh probe and can attach the previous matching observation if it fails.
     * @return current observation, or a clearly marked stale snapshot or failure
     */
    public OpenCliCapabilitySnapshot refresh() {
        return refresh(OpenCliHelpScope.ROOT_HELP);
    }

    /**
     * Queries a fixed help scope on the pinned protocol version and tracks its snapshot.
     * @param scope help query from the locked protocol fixture set
     * @return current scoped observation or the explicit stale or failure status
     */
    public OpenCliCapabilitySnapshot refresh(OpenCliHelpScope scope) {
        Objects.requireNonNull(scope, "scope");
        long attempt;
        synchronized (this) {
            attempt = ++nextAttempt;
        }
        return publish(attempt, observe(scope));
    }

    private synchronized OpenCliCapabilitySnapshot publish(long attempt, OpenCliCapabilitySnapshot current) {
        if (attempt < lastPublishedAttempt) {
            return current;
        }
        lastPublishedAttempt = attempt;
        lastAvailable.values().removeIf(old ->
            !old.getConfigurationSha256().equals(current.getConfigurationSha256())
                || (Objects.nonNull(current.getObservedVersion())
                    && !old.getObservedVersion().equals(current.getObservedVersion())));
        OpenCliHelpScope scope = current.getHelpScope();
        if (current.getStatus() == Status.AVAILABLE) {
            lastAvailable.put(scope, current);
            return current;
        }
        OpenCliCapabilitySnapshot previous = lastAvailable.get(scope);
        if (current.getStatus() == Status.UNKNOWN && Objects.nonNull(previous)) {
            return previous.toBuilder().status(Status.STALE).reason(current.getReason())
                .failedRefresh(current).build();
        }
        lastAvailable.remove(scope);
        return current;
    }

    private OpenCliCapabilitySnapshot observe(OpenCliHelpScope scope) {
        OpenCliProperties configured = executor.getProperties();
        OpenCliCapabilitySnapshot.OpenCliCapabilitySnapshotBuilder snapshot =
            OpenCliCapabilitySnapshot.builder().helpScope(scope).observedAt(Instant.now());
        if (configured.getExecutionTarget() != OpenCliExecutionTarget.LOCAL_PROCESS) {
            return snapshot.status(Status.UNSUPPORTED).reason("LOCAL_DISCOVERY_ONLY").build();
        }
        if (OpenCliStrings.isBlank(configured.getExecutable())) {
            return snapshot.status(Status.UNSUPPORTED).reason("ABSOLUTE_EXECUTABLE_REQUIRED").build();
        }

        OpenCliProperties probe = configured.copyForLocalCliExecution();
        Path executable;
        try {
            Path configuredPath = Paths.get(probe.getExecutable().trim());
            if (!configuredPath.isAbsolute()) {
                return snapshot.status(Status.UNSUPPORTED).reason("ABSOLUTE_EXECUTABLE_REQUIRED").build();
            }
            executable = configuredPath.toRealPath();
            if (!Files.isRegularFile(executable) || !Files.isExecutable(executable)) {
                return snapshot.status(Status.UNKNOWN).reason("EXECUTABLE_UNAVAILABLE").build();
            }
        } catch (IOException | InvalidPathException | SecurityException ex) {
            return snapshot.status(Status.UNKNOWN).reason("EXECUTABLE_UNAVAILABLE").build();
        }

        probe.setExecutable(executable.toString());
        probe.setCommandTimeoutMillis(Math.min(MAX_PROBE_MILLIS,
            Math.min(probe.getStartupProbeTimeoutMillis(), probe.getCommandTimeoutMillis())));
        probe.setCleanupGraceMillis(Math.min(MAX_CLEANUP_MILLIS, probe.getCleanupGraceMillis()));
        probe.setMaxStdoutBytes(Math.min(MAX_PROBE_OUTPUT_BYTES, probe.getMaxStdoutBytes()));
        probe.setMaxStderrBytes(Math.min(MAX_PROBE_OUTPUT_BYTES, probe.getMaxStderrBytes()));
        snapshot.executable(probe.getExecutable()).configurationSha256(configurationDigest(probe));
        try (OpenCliExecutor local = new OpenCliExecutor(probe, executor.getProcessRuntime())) {
            String phase = "VERSION";
            try {
                OpenCliResult versionResult = local.invokeLocalOnly(Collections.singletonList("--version"));
                Matcher versionOutput = VERSION.matcher(versionResult.getStdout());
                if (versionResult.getExecutionDetails().isStdoutTruncated() || !versionOutput.matches()) {
                    return snapshot.status(Status.UNKNOWN).reason("INVALID_VERSION_OUTPUT").build();
                }
                String version = versionOutput.group(1);
                snapshot.observedVersion(version);
                if (scope != OpenCliHelpScope.ROOT_HELP && !"1.8.8".equals(version)) {
                    return snapshot.status(Status.UNSUPPORTED).reason("UNVERIFIED_HELP_VERSION").build();
                }
                phase = "HELP";
                OpenCliResult helpResult = local.invokeLocalOnly(scope.arguments());
                if (helpResult.getExecutionDetails().isStdoutTruncated()) {
                    return snapshot.status(Status.UNKNOWN).reason("HELP_TRUNCATED").build();
                }
                String stdout = helpResult.getStdout();
                OpenCliHelpResult help = OpenCliHelpParser.parse(stdout);
                snapshot.help(help).helpSha256(hex(sha256().digest(stdout.getBytes(StandardCharsets.UTF_8))));
                if (help.getStatus() == OpenCliHelpResult.Status.AVAILABLE) {
                    return scope.matches(help.getSchema()) ? snapshot.status(Status.AVAILABLE).build()
                        : snapshot.status(Status.UNKNOWN).reason("UNEXPECTED_HELP_SCOPE").build();
                }
                return snapshot.status(Status.UNKNOWN).reason(help.getReason().name()).build();
            } catch (OpenCliTimeoutException ex) {
                return snapshot.status(Status.UNKNOWN).reason(phase + "_TIMEOUT").build();
            } catch (OpenCliException ex) {
                OpenCliResult partial = ex.getPartialResult();
                if (Objects.nonNull(partial) && partial.getExecutionDetails().getTerminationReason() == TerminationReason.OUTPUT_LIMIT) {
                    return snapshot.status(Status.UNKNOWN).reason(phase + "_TRUNCATED").build();
                }
                return snapshot.status(Status.UNKNOWN).reason(phase + "_PROBE_FAILED").build();
            } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException ex) {
                return snapshot.status(Status.UNKNOWN).reason(phase + "_PROBE_FAILED").build();
            }
        }
    }

    private static String configurationDigest(OpenCliProperties probe) {
        MessageDigest digest = sha256();
        add(digest, probe.getExecutable());
        String directory = probe.getWorkingDirectory();
        add(digest, OpenCliStrings.isBlank(directory) ? System.getProperty("user.dir") : directory.trim());
        add(digest, Integer.toString(probe.getLeadingArguments().size()));
        for (String argument : probe.getLeadingArguments()) {
            add(digest, argument);
        }
        Map<String, String> environment = new TreeMap<>(System.getenv());
        probe.getEnvironment().forEach((key, value) -> {
            if (Objects.nonNull(key) && Objects.nonNull(value)) {
                environment.put(key, value);
            }
        });
        environment.forEach((key, value) -> {
            add(digest, key);
            add(digest, value);
        });
        return hex(digest.digest());
    }

    private static void add(MessageDigest digest, String value) {
        if (Objects.isNull(value)) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("The Java runtime must provide SHA-256", ex);
        }
    }

    private static String hex(byte[] digest) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] result = new char[digest.length * 2];
        for (int index = 0; index < digest.length; index++) {
            int value = digest[index] & 0xff;
            result[index * 2] = digits[value >>> 4];
            result[index * 2 + 1] = digits[value & 0xf];
        }
        return new String(result);
    }
}
