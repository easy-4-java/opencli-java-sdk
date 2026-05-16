package io.github.hiwepy.opencli.adapter.publicapi.binance;

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
 * OpenCLI {@code binance} 公共行情适配器。
 */
@RequiredArgsConstructor
public final class BinanceOpenCliClient {

    private final OpenCliExecutor executor;

    private OpenCliAdapterChannel ch() {
        return new OpenCliAdapterChannel(executor, OpenCliAdapterIds.BINANCE);
    }

    public OpenCliResult price(String symbol, List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("price", symbol), more));
    }

    public OpenCliResult prices(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("prices"), more));
    }

    public OpenCliResult ticker(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("ticker"), more));
    }

    public OpenCliResult pairs(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("pairs"), more));
    }

    public OpenCliResult trades(String symbol, Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("trades", symbol));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult depth(String symbol, Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("depth", symbol));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult asks(String symbol, Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("asks", symbol));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult klines(String symbol, String interval, Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("klines", symbol));
        if (interval != null) {
            OpenCliArgSupport.addOptionPair(args, "--interval", interval);
        }
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult top(boolean json, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("top"));
        if (json) {
            args.add("-f");
            args.add("json");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    public OpenCliResult gainers(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("gainers"), more));
    }

    public OpenCliResult losers(List<String> more) {
        return ch().invoke(OpenCliArgSupport.merge(OpenCliLists.of("losers"), more));
    }

    public OpenCliTypedResult<JsonNode> topTyped(List<String> more) {
        return OpenCliStdoutJson.typed(top(true, more));
    }
}
