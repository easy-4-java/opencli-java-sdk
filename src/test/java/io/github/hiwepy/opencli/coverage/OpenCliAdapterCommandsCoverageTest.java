package io.github.hiwepy.opencli.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import io.github.hiwepy.opencli.core.OpenCliAdapterCommandRequest;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * cli-manifest.json 全部 adapter 子命令覆盖（共 899 条）。
 * <p>
 * <strong>成功标准：</strong>通过 {@link OpenCliAdapterCommandRequest} +
 * {@link OpenCliAdapterChannel#invoke(OpenCliAdapterCommandRequest)} 发起调用且返回非 null
 * {@link OpenCliResult}；argv 以 site id 与子命令开头。禁止测试中手工 argv 拼接。
 * 已有 typed client + Options 的 argv 映射见 {@link OpenCliTypedClientOptionsCoverageTest}。
 * </p>
 * <p>由 {@code scripts/generate_opencli_command_tests.py} 生成，请勿手改。</p>
 */
class OpenCliAdapterCommandsCoverageTest {

    private static final int EXPECTED_MANIFEST_COMMAND_COUNT = 899;

    private static class ManifestCommand {
        private String site;
        private String subcommand;
        private List<String> positionals;
        private Map<String, Object> options;

        public ManifestCommand() {}

        public String getSite() { return site; }
        public void setSite(String site) { this.site = site; }
        public String getSubcommand() { return subcommand; }
        public void setSubcommand(String subcommand) { this.subcommand = subcommand; }
        public List<String> getPositionals() { return positionals; }
        public void setPositionals(List<String> positionals) { this.positionals = positionals; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    static Stream<Arguments> manifestCommands() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = OpenCliAdapterCommandsCoverageTest.class.getResourceAsStream(
            "/opencli/manifest-coverage-commands.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /opencli/manifest-coverage-commands.json");
            }
            List<ManifestCommand> rows = mapper.readValue(in, new TypeReference<List<ManifestCommand>>() {});
            return rows.stream()
                .map(r -> Arguments.of(r.getSite(), r.getSubcommand(), r.getPositionals(), r.getOptions()));
        }
    }

    @Test
    void manifestResourceCountMatchesExpected() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = OpenCliAdapterCommandsCoverageTest.class.getResourceAsStream(
            "/opencli/manifest-coverage-commands.json")) {
            List<ManifestCommand> rows = mapper.readValue(in, new TypeReference<List<ManifestCommand>>() {});
            assertEquals(EXPECTED_MANIFEST_COMMAND_COUNT, rows.size());
        }
    }

    @ParameterizedTest(name = "{0}/{1}")
    @MethodSource("manifestCommands")
    void adapterCommand(
        String site,
        String subcommand,
        List<String> positionals,
        Map<String, Object> options) {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        OpenCliAdapterChannel channel = new OpenCliAdapterChannel(exec, site);
        OpenCliAdapterCommandRequest request = OpenCliAdapterCommandRequest.builder()
            .subcommand(subcommand)
            .positionals(positionals != null ? positionals : java.util.Collections.emptyList())
            .options(options != null ? options : java.util.Collections.emptyMap())
            .build();
        OpenCliResult result = channel.invoke(request);
        assertNotNull(result);
        List<String> argv = exec.lastInvocation();
        assertFalse(argv.isEmpty());
        assertEquals(site, argv.get(0));
        assertEquals(subcommand, argv.get(1));
    }
}
