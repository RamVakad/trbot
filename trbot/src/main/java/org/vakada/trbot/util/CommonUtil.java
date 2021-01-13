package org.vakada.trbot.util;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import lombok.extern.slf4j.Slf4j;
import org.vakada.trbot.common.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Slf4j
public class CommonUtil {

    public static double roundUp(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.UP);
        return bd.doubleValue();
    }

    public static double roundDown(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.DOWN);
        return bd.doubleValue();
    }

    public static Double getSpreadBeaterAmount(int modifier) {
        Double mult = Math.min(1.0 + ((double) modifier) / 10, 2.0);
        return (((double) new Random().nextInt(10)) / 1000) * mult;
    }

    public static Double getHighestBid(BinanceApiRestClient client) {
        OrderBook orderBook = client.getOrderBook(Constants.PAIR, 5);
        List<OrderBookEntry> bids = orderBook.getBids();
        OrderBookEntry firstBidEntry = bids.get(0);
        return Double.parseDouble(firstBidEntry.getPrice());
    }

    public static Double getLowestAsk(BinanceApiRestClient client) {
        OrderBook orderBook = client.getOrderBook(Constants.PAIR, 5);
        List<OrderBookEntry> asks = orderBook.getAsks();
        OrderBookEntry firstAsk = asks.get(0);
        return Double.parseDouble(firstAsk.getPrice());
    }
}
