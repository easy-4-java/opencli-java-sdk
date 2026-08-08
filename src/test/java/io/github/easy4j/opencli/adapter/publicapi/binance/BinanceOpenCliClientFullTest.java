package io.github.easy4j.opencli.adapter.publicapi.binance;

import static org.junit.jupiter.api.Assertions.*;
import io.github.easy4j.opencli.core.OpenCliResult;
import io.github.easy4j.opencli.support.RecordingOpenCliExecutor;
import java.util.*;
import org.junit.jupiter.api.Test;

class BinanceOpenCliClientFullTest {

    private final RecordingOpenCliExecutor exec = new RecordingOpenCliExecutor();
    private final BinanceOpenCliClient client = new BinanceOpenCliClient(exec);

    @Test void shouldCallPrice() { assertNotNull(client.price("BTCUSDT", null)); }
    @Test void shouldCallPricesWithLimit() { assertNotNull(client.prices(10, null)); }
    @Test void shouldCallPricesNoLimit() { assertNotNull(client.prices(null)); }
    @Test void shouldCallTickerWithLimit() { assertNotNull(client.ticker(10, null)); }
    @Test void shouldCallTickerNoLimit() { assertNotNull(client.ticker(null)); }
    @Test void shouldCallPairsWithLimit() { assertNotNull(client.pairs(10, null)); }
    @Test void shouldCallPairsNoLimit() { assertNotNull(client.pairs(null)); }
    @Test void shouldCallTrades() { assertNotNull(client.trades("BTCUSDT", 10, null)); }
    @Test void shouldCallDepth() { assertNotNull(client.depth("BTCUSDT", 10, null)); }
    @Test void shouldCallAsks() { assertNotNull(client.asks("BTCUSDT", 10, null)); }
    @Test void shouldCallKlines() { assertNotNull(client.klines("BTCUSDT", "1h", 10, null)); }
    @Test void shouldCallTopWithJson() { assertNotNull(client.top(10, true, null)); }
    @Test void shouldCallTopNoLimit() { assertNotNull(client.top(true, null)); }
    @Test void shouldCallGainersWithLimit() { assertNotNull(client.gainers(10, null)); }
    @Test void shouldCallGainersNoLimit() { assertNotNull(client.gainers(null)); }
    @Test void shouldCallLosersWithLimit() { assertNotNull(client.losers(10, null)); }
    @Test void shouldCallLosersNoLimit() { assertNotNull(client.losers(null)); }
    @Test void shouldCallTopTyped() { assertNotNull(client.topTyped(10, null)); }
    @Test void shouldCallTopTypedNoLimit() { assertNotNull(client.topTyped(null)); }
}
