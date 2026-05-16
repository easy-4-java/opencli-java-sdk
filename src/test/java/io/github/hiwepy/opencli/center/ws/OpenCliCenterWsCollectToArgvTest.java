package io.github.hiwepy.opencli.center.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link OpenCliCenterWsCollectToArgv} 与 Python {@code agent_server.collect} 拼装规则对齐性测试。
 */
class OpenCliCenterWsCollectToArgvTest {

    @Test
    void buildsArgvLikeAgentServer() {
        List<String> argv =
            OpenCliCenterWsCollectToArgv.toArgv(
                "npm",
                "package",
                List.of("react"),
                Map.of("limit", 5),
                "json");
        assertEquals(List.of("npm", "package", "react", "--limit", "5", "-f", "json"), argv);
    }

    @Test
    void rejectsBlankSite() {
        assertThrows(
            IllegalArgumentException.class,
            () ->
                OpenCliCenterWsCollectToArgv.toArgv("", "cmd", List.of(), Map.of(), "json"));
    }
}
