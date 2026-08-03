package io.github.easy4j.opencli.center.ws;

import io.github.easy4j.opencli.util.OpenCliLists;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.easy4j.opencli.util.OpenCliLists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link OpenCliCenterWsCollectToArgv} 与 Python {@code agent_server.collect} 拼装规则对齐性测试。
 */
class OpenCliCenterWsCollectToArgvTest {

    @Test
    void buildsArgvLikeAgentServer() {
        Map<String, Object> args = new HashMap<>();
        args.put("limit", 5);
        List<String> argv =
            OpenCliCenterWsCollectToArgv.toArgv(
                "npm",
                "package",
                OpenCliLists.of("react"),
                args,
                "json");
        assertEquals(OpenCliLists.of("npm", "package", "react", "--limit", "5", "-f", "json"), argv);
    }

    @Test
    void rejectsBlankSite() {
        assertThrows(
            IllegalArgumentException.class,
            () ->
                OpenCliCenterWsCollectToArgv.toArgv(
                    "", "cmd", OpenCliLists.of(), new HashMap<String, Object>(), "json"));
    }
}
