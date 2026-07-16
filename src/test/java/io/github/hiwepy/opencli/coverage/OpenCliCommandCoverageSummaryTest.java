package io.github.hiwepy.opencli.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * OpenCLI Java SDK 命令覆盖计数汇总。
 * <p>
 * manifest adapter 子命令 + meta 根命令 + browser 子命令 = SDK 侧需覆盖的全部 CLI 入口。
 * </p>
 */
class OpenCliCommandCoverageSummaryTest {

    /** cli-manifest.json 条目数（adapter 子命令）。 */
    static final int MANIFEST_ADAPTER_COMMANDS = 1275;

    /** 根级 / meta 子命令数。 */
    static final int META_COMMANDS = 30;

    /** browser 叶子子命令数。 */
    static final int BROWSER_COMMANDS = 42;

    /** 合计需覆盖命令数。 */
    static final int TOTAL_COMMANDS = 1347;

    @Test
    void manifestResourceMatchesConstant() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(
            OpenCliCommandCoverageSummaryTest.class.getResourceAsStream("/opencli/manifest-coverage-meta.json"));
        assertEquals(MANIFEST_ADAPTER_COMMANDS, root.get("manifestCommandCount").asInt());
    }

    @Test
    void totalCoverageBudget() {
        assertEquals(1347, MANIFEST_ADAPTER_COMMANDS + META_COMMANDS + BROWSER_COMMANDS);
    }
}
