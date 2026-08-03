package io.github.easy4j.opencli.adapter.publicapi.binance;

import io.github.easy4j.opencli.util.OpenCliLists;
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

    public OpenCliResult prices(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("prices"));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #prices(Integer, List) */
    public OpenCliResult prices(List<String> more) {
        return prices(null, more);
    }

    public OpenCliResult ticker(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("ticker"));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #ticker(Integer, List) */
    public OpenCliResult ticker(List<String> more) {
        return ticker(null, more);
    }

    public OpenCliResult pairs(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("pairs"));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #pairs(Integer, List) */
    public OpenCliResult pairs(List<String> more) {
        return pairs(null, more);
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

    public OpenCliResult top(Integer limit, boolean json, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("top"));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        if (json) {
            args.add("-f");
            args.add("json");
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #top(Integer, boolean, List) */
    public OpenCliResult top(boolean json, List<String> more) {
        return top(null, json, more);
    }

    public OpenCliResult gainers(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("gainers"));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #gainers(Integer, List) */
    public OpenCliResult gainers(List<String> more) {
        return gainers(null, more);
    }

    public OpenCliResult losers(Integer limit, List<String> more) {
        List<String> args = new ArrayList<>(OpenCliLists.of("losers"));
        if (limit != null) {
            OpenCliArgSupport.addOptionPair(args, "--limit", String.valueOf(limit));
        }
        return ch().invoke(OpenCliArgSupport.merge(args, more));
    }

    /** @see #losers(Integer, List) */
    public OpenCliResult losers(List<String> more) {
        return losers(null, more);
    }

    public OpenCliTypedResult<JsonNode> topTyped(Integer limit, List<String> more) {
        return OpenCliStdoutJson.typed(top(limit, true, more));
    }

    /** @see #topTyped(Integer, List) */
    public OpenCliTypedResult<JsonNode> topTyped(List<String> more) {
        return topTyped(null, more);
    }
}
