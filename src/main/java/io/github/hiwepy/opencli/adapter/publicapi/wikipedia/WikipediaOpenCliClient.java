package io.github.hiwepy.opencli.adapter.publicapi.wikipedia;

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
        List<String> args = new ArrayList<>(List.of("summary", title));
        if (lang != null) {
            OpenCliArgSupport.addOptionPair(args, "--lang", lang);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult random(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(List.of("random"), more));
    }

    public OpenCliResult trending(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(List.of("trending"), more));
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
