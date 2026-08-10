package io.github.easy4j.opencli.adapter.publicapi.npm;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import io.github.easy4j.opencli.core.OpenCliArgSupport;
import io.github.easy4j.opencli.core.OpenCliExecutor;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.core.OpenCliTypedResult;
import io.github.easy4j.opencli.parser.OpenCliStdoutJson;
import io.github.easy4j.opencli.registry.OpenCliAdapterIds;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * OpenCLI {@code npm} 公共 registry 适配器。
 */
@RequiredArgsConstructor/**

 * OpenCLI {@code npm} public registry adapter client.
 *
 * <p>Provides methods to search npm packages, retrieve package info,
 * and query download statistics.</p>

 *

 * @author <a href="https://github.com/loong10k">Loong Wan</a>

 * @since 3.0.0

 */

public final class NpmOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.NPM);
    }

    public OpenCliResult search(String query, Integer limit, boolean json, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("search");
        args.add(query);
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        if (json) {
            args.add("-f");
            args.add("json");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult packageInfo(String packageName, boolean json, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("package");
        args.add(packageName);
        if (json) {
            args.add("-f");
            args.add("json");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /**
     * @param period 命名周期；若为 null 则使用 CLI 默认（文档为 last-week）
     */
    public OpenCliResult downloads(String packageName, NpmDownloadPeriod period, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("downloads");
        args.add(packageName);
        if (period != null) {
            OpenCliArgSupport.addOptionPair(args, "--period", period.getCliValue());
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /**
     * @param range 自定义 {@code YYYY-MM-DD:YYYY-MM-DD}
     */
    public OpenCliResult downloads(String packageName, String range, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("downloads");
        args.add(packageName);
        if (range != null) {
            OpenCliArgSupport.addOptionPair(args, "--period", range);
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliTypedResult<JsonNode> packageInfoTyped(String packageName, List<String> more) {
        OpenCliResult raw = packageInfo(packageName, true, more);
        return OpenCliStdoutJson.typed(raw);
    }

    public OpenCliTypedResult<JsonNode> searchTyped(String query, Integer limit, List<String> more) {
        return OpenCliStdoutJson.typed(search(query, limit, true, more));
    }

    public OpenCliTypedResult<JsonNode> downloadsTyped(String packageName, NpmDownloadPeriod period, List<String> more) {
        List<String> args = new ArrayList<>();
        args.add("downloads");
        args.add(packageName);
        if (period != null) {
            OpenCliArgSupport.addOptionPair(args, "--period", period.getCliValue());
        }
        args.add("-f");
        args.add("json");
        return OpenCliStdoutJson.typed(ch().invoke(OpenCliArgSupport.merge(args, more)));
    }
}
