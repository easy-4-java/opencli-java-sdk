package io.github.hiwepy.opencli.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hiwepy.opencli.adapter.publicapi.npm.NpmOpenCliClient;
import io.github.hiwepy.opencli.support.RecordingOpenCliExecutor;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 无 Options 类的强类型 adapter argv 抽查。
 * <p>含 Options 的 browser LLM 适配器见 {@link io.github.hiwepy.opencli.coverage.OpenCliTypedClientOptionsCoverageTest}。</p>
 */
class TypedAdapterArgvTest {

    @Test
    void npmPackageInfoJson() {
        RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
        new NpmOpenCliClient(exec).packageInfo("react", true, null);
        assertEquals(List.of("npm", "package", "react", "-f", "json"), exec.lastInvocation());
    }
}
