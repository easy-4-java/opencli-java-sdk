package io.github.easy4j.opencli.facade;

import io.github.easy4j.opencli.OpenCliClient;
import io.github.easy4j.opencli.adapter.publicapi.arxiv.ArxivOpenCliClient;
import io.github.easy4j.opencli.adapter.publicapi.binance.BinanceOpenCliClient;
import io.github.easy4j.opencli.adapter.publicapi.npm.NpmOpenCliClient;
import io.github.easy4j.opencli.adapter.publicapi.pypi.PypiOpenCliClient;
import io.github.easy4j.opencli.adapter.publicapi.wikipedia.WikipediaOpenCliClient;
import io.github.easy4j.opencli.core.OpenCliAdapterChannel;
import lombok.Getter;

/**
 * Public HTTP/API adapter entry point (no browser required).
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 */public class PublicApiClient {

    @Getter
    private final OpenCliClient openCli;

    public PublicApiClient(OpenCliClient openCli) {
        this.openCli = openCli;
    }

    public OpenCliAdapterChannel channel(String adapterId) {
        return openCli.adapter(adapterId);
    }

    public ArxivOpenCliClient arxiv() {
        return openCli.arxiv();
    }

    public NpmOpenCliClient npm() {
        return openCli.npm();
    }

    public PypiOpenCliClient pypi() {
        return openCli.pypi();
    }

    public BinanceOpenCliClient binance() {
        return openCli.binance();
    }

    public WikipediaOpenCliClient wikipedia() {
        return openCli.wikipedia();
    }
}
