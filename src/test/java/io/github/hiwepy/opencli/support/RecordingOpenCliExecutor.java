package io.github.hiwepy.opencli.support;

import io.github.hiwepy.opencli.OpenCliProperties;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.parser.OpenCliParsedFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试用：记录 {@link #invoke(List)} 的 argv，不启动真实子进程。
 */
public final class RecordingOpenCliExecutor extends OpenCliExecutor {

    private final CopyOnWriteArrayList<List<String>> invocations = new CopyOnWriteArrayList<>();

    public RecordingOpenCliExecutor() {
        super(new OpenCliProperties());
    }

    public List<List<String>> getInvocations() {
        return Collections.unmodifiableList(new ArrayList<>(invocations));
    }

    public List<String> lastInvocation() {
        List<List<String>> all = getInvocations();
        return all.isEmpty() ? List.of() : all.get(all.size() - 1);
    }

    @Override
    public OpenCliResult invoke(List<String> adapterAndRest) {
        invocations.add(new ArrayList<>(adapterAndRest));
        return OpenCliResult.builder()
            .stdout("[]")
            .stderr("")
            .exitCode(0)
            .success(true)
            .parsed(OpenCliParsedFields.builder().build())
            .build();
    }
}
