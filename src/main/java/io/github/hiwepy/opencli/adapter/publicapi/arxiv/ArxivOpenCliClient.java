package io.github.hiwepy.opencli.adapter.publicapi.arxiv;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.hiwepy.opencli.core.OpenCliAdapterChannel;
import io.github.hiwepy.opencli.core.OpenCliArgSupport;
import io.github.hiwepy.opencli.core.OpenCliExecutor;
import io.github.hiwepy.opencli.core.OpenCliResult;
import io.github.hiwepy.opencli.core.OpenCliTypedResult;
import io.github.hiwepy.opencli.parser.OpenCliStdoutJson;
import io.github.hiwepy.opencli.registry.OpenCliAdapterIds;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code arxiv} 公共 API 适配器。
 */
@RequiredArgsConstructor
public final class ArxivOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.ARXIV);
    }

    /**
     * @param query 检索关键词
     * @param limit 最大条数，可为 null
     * @param json  是否 {@code -f json}
     * @param more  透传，可为 null
     */
    public OpenCliResult search(String query, Integer limit, boolean json, List<String> more) {
        List<String> args = baseList("search", query, limit, json);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** 论文详情。 */
    public OpenCliResult paper(String arxivId, boolean json, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("paper");
        args.add(arxivId);
        if (json) {
            args.add("-f");
            args.add("json");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** 分类下最新论文。 */
    public OpenCliResult recent(String category, Integer limit, boolean json, List<String> more) {
        List<String> args = baseList("recent", category, limit, json);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** 作者论文列表。 */
    public OpenCliResult author(String name, Integer limit, boolean json, List<String> more) {
        List<String> args = baseList("author", name, limit, json);
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /**
     * 同 {@link #search(String, Integer, boolean, List)}，并在 {@code json=true} 时解析 stdout。
     */
    public OpenCliTypedResult<JsonNode> searchTyped(String query, Integer limit, List<String> more) {
        OpenCliResult raw = search(query, limit, true, more);
        return OpenCliStdoutJson.typed(raw);
    }

    public OpenCliTypedResult<JsonNode> paperTyped(String arxivId, List<String> more) {
        return OpenCliStdoutJson.typed(paper(arxivId, true, more));
    }

    public OpenCliTypedResult<JsonNode> recentTyped(String category, Integer limit, List<String> more) {
        return OpenCliStdoutJson.typed(recent(category, limit, true, more));
    }

    private static List<String> baseList(String sub, String positional, Integer limit, boolean json) {
        List<String> args = new ArrayList<>();
        args.add(sub);
        args.add(positional);
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        if (json) {
            args.add("-f");
            args.add("json");
        }
        return args;
    }
}
