package org.vakada.trbot.common;


import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.account.AssetBalance;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.vakada.trbot.worker.BasicStreamer;

@Getter
@Setter
@Slf4j
public class CommonState {

    public static CommonState instance;

    private BinanceApiClientFactory clientFactory;
    private BasicStreamer basicStreamer;

    private Double price = -1.0;
    private Double highestBid = -1.0;
    private Double lowestAsk = -1.0;
    private Double lastBuy = -1.0;

    private AssetBalance usdtBalance;
    private AssetBalance pairBalance;

    private CommonState() {
        log.info("Initializing CommonState");
        this.clientFactory = BinanceApiClientFactory.newInstance(Constants.API_KEY, Constants.API_SECRET);
    }

    public static CommonState getInstance() {
        if (instance == null) {
            instance = new CommonState();
        }
        return instance;
    }

    public void startWorkers() {
        log.info("Starting Workers");
        basicStreamer = new BasicStreamer();
        basicStreamer.start();
    }
}
