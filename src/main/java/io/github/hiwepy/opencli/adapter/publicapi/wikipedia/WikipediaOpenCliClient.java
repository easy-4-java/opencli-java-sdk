package io.github.hiwepy.opencli.adapter.publicapi.wikipedia;

import io.github.hiwepy.opencli.util.OpenCliLists;
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
 * OpenCLI {@code wikipedia} 公共 API 适配器。
 */
@RequiredArgsConstructor
public final class WikipediaOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.WIKIPEDIA);
    }

    public OpenCliResult search(String query, Integer limit, String lang, boolean json, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("search");
        args.add(query);
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        if (lang != null) {
            OpenCliArgSupport.addOptionPair(args, "--lang", lang);
        }
        if (json) {
            args.add("-f");
            args.add("json");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult summary(String title, String lang, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("summary", title));
        if (lang != null) {
            OpenCliArgSupport.addOptionPair(args, "--lang", lang);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /**
     * 随机条目。
     *
     * @param lang 维基语言代码（如 {@code en}、{@code zh}），可为 null
     * @param more 透传参数，可为 null
     */
    public OpenCliResult random(String lang, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("random");
        if (lang != null) {
            OpenCliArgSupport.addOptionPair(args, "--lang", lang);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #random(String, List) */
    public OpenCliResult random(List<String> more) {
        return random(null, more);
    }

    /**
     * 热门条目排行。
     *
     * @param limit 最大条数，可为 null
     * @param lang  语言代码，可为 null
     * @param more  透传参数，可为 null
     */
    public OpenCliResult trending(Integer limit, String lang, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("trending");
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        if (lang != null) {
            OpenCliArgSupport.addOptionPair(args, "--lang", lang);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #trending(Integer, String, List) */
    public OpenCliResult trending(List<String> more) {
        return trending(null, null, more);
    }

    public OpenCliResult page(String title, String lang, Integer paragraphs, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("page");
        args.add(title);
        if (lang != null) {
            OpenCliArgSupport.addOptionPair(args, "--lang", lang);
        }
        if (paragraphs != null) {
            OpenCliArgSupport.addOptionPair(args, "--paragraphs", String.valueOf(paragraphs));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliTypedResult<JsonNode> searchTyped(String query, Integer limit, String lang, List<String> more) {
        OpenCliResult raw = search(query, limit, lang, true, more);
        return OpenCliStdoutJson.typed(raw);
    }
}
