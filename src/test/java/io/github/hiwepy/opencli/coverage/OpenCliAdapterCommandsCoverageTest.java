package io.github.hiwepy.opencli.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * cli-manifest.json 全部 adapter 子命令覆盖（共 899 条）。
 * <p>
 * <strong>成功标准：</strong>通过 {@link OpenCliAdapterChannel#invoke} 发起调用且返回非 null
 * {@link OpenCliResult}；argv 以 site id 与子命令开头。不校验业务输出、登录态或平台可用性。
 * </p>
 * <p>由 {@code scripts/generate_opencli_command_tests.py} 生成，请勿手改。</p>
 */
class OpenCliAdapterCommandsCoverageTest {

    private static final int EXPECTED_MANIFEST_COMMAND_COUNT = 899;

    private record ManifestCommand(String site, String subcommand, List<String> invokeArgs) {}

    static Stream<Arguments> manifestCommands() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = OpenCliAdapterCommandsCoverageTest.class.getResourceAsStream(
            "/opencli/manifest-coverage-commands.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /opencli/manifest-coverage-commands.json");
            }
            List<ManifestCommand> rows = mapper.readValue(in, new TypeReference<>() {});
            return rows.stream()
                .map(r -> Arguments.of(r.site(), r.subcommand(), r.invokeArgs()));
        }
    }

    @Test
    void manifestResourceCountMatchesExpected() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = OpenCliAdapterCommandsCoverageTest.class.getResourceAsStream(
            "/opencli/manifest-coverage-commands.json")) {
            List<ManifestCommand> rows = mapper.readValue(in, new TypeReference<>() {});
            assertEquals(EXPECTED_MANIFEST_COMMAND_COUNT, rows.size());
        }
    }

    @ParameterizedTest(name = "{0}/{1}")
    @MethodSource("manifestCommands")
    void adapterCommand(String site, String subcommand, List<String> invokeArgs) {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliAdapterChannel channel = new OpenCliAdapterChannel(exec, site);
        OpenCliResult result = channel.invoke(invokeArgs);
        assertNotNull(result);
        List<String> argv = exec.lastInvocation();
        assertFalse(argv.isEmpty());
        assertEquals(site, argv.get(0));
        assertEquals(subcommand, argv.get(1));
    }
}
