package org.vakada.trbot.worker;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.binance.api.client.domain.market.TickerStatistics;
import lombok.extern.slf4j.Slf4j;
import org.vakada.trbot.common.CommonState;
import org.vakada.trbot.common.Constants;
import org.vakada.trbot.util.CommonUtil;

import java.util.List;

@Slf4j
public class BasicStreamer extends Thread {

    private BinanceApiAsyncRestClient asyncRestClient;


    public BasicStreamer() {
        this.asyncRestClient = CommonState.getInstance().getClientFactory().newAsyncRestClient();
    }

    public void run() {
        while (true) {
            //log.info("Refreshing UI Values ...");
            try {
                asyncRestClient.getMyTrades(Constants.PAIR, 5, (List<Trade> trades) -> {
                    for(int i = trades.size() - 1; i >= 0; i--) {
                        if (trades.get(i).isBuyer()) {
                            CommonState.getInstance().setLastBuy(CommonUtil.roundUp(Double.parseDouble(trades.get(i).getPrice()), 3));
                            break;
                        }
                    }
                });
                asyncRestClient.getAccount((Account account) -> {
                    AssetBalance usdtBalance = account.getAssetBalance("USDT");
                    CommonState.getInstance().setUsdtBalance(usdtBalance);
                    AssetBalance pairBalance = account.getAssetBalance(Constants.SYMBOL);
                    CommonState.getInstance().setPairBalance(pairBalance);
                });

                asyncRestClient.get24HrPriceStatistics(Constants.PAIR, (TickerStatistics tick) -> {
                    Double lastPrice = Double.parseDouble(tick.getLastPrice());
                    CommonState.getInstance().setPrice(lastPrice);
                    //log.debug("{} last price: ${}", Constants.TICKER, lastPrice);
                });


                asyncRestClient.getOrderBook(Constants.PAIR, 5, (OrderBook orderBook) -> {
                    List<OrderBookEntry> asks = orderBook.getAsks();
                    OrderBookEntry firstAskEntry = asks.get(0);
                    Double lowestAsk = Double.parseDouble(firstAskEntry.getPrice());
                    CommonState.getInstance().setLowestAsk(lowestAsk);

                    //log.debug("{} lowest ask: ${}", Constants.TICKER, lowestAsk);

                    List<OrderBookEntry> bids = orderBook.getBids();
                    OrderBookEntry firstBidEntry = bids.get(0);
                    Double highestBid = Double.parseDouble(firstBidEntry.getPrice());
                    CommonState.getInstance().setHighestBid(highestBid);

                    //log.debug("{} highest bid: ${}", Constants.TICKER, highestBid);
                });

                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("Error: ", e);
            }
        }

    }
}