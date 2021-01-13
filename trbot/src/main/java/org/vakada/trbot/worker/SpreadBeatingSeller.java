package org.vakada.trbot.worker;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import lombok.extern.slf4j.Slf4j;
import org.vakada.trbot.common.CommonState;
import org.vakada.trbot.common.Constants;
import org.vakada.trbot.util.CommonUtil;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SpreadBeatingSeller implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private Double sellAmount;
    private BinanceApiRestClient client;
    private Integer mod;
    private Long orderId;

    public SpreadBeatingSeller(Double sellAmount) {
        this.sellAmount = sellAmount;
        this.client = CommonState.getInstance().getClientFactory().newRestClient();
        this.mod = 1;
    }

    public void start() {
        log.info("--------------------------------------------------------------------------------------------");
        worker = new Thread(this);
        worker.start();
    }

    public void stop() {
        worker.interrupt();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void run() {
        running.set(true);
        try {
            CommonUtil.roundDown(sellAmount, 2);

            log.info("Performing Beater Sell, sellAmount: {}, mod: {}", sellAmount, mod);


            client = CommonState.getInstance().getClientFactory().newRestClient();

            Double symbolBalance = Double.parseDouble(client.getAccount().getAssetBalance(Constants.SYMBOL).getFree());
            if (sellAmount > symbolBalance) {
                log.warn("Sell Quantity is greater than available balance ... failing out");
                return;
            }

            if (sellAmount == -1.0) {
                sellAmount = CommonUtil.roundDown(symbolBalance, 2);
            }

            Double lowestAsk = CommonUtil.getLowestAsk(client);
            log.info("Current Lowest Ask for {}: ${}", Constants.PAIR, lowestAsk);
            Double randDec = CommonUtil.getSpreadBeaterAmount(mod);
            log.info("Calculated Spread Beater: ${}", randDec);
            Double ourAsk = CommonUtil.roundUp(lowestAsk - randDec, 3);
            log.info("Setting our ask to ${}", ourAsk);

            if (ourAsk * sellAmount < 10) {
                log.info("Sell Amount is < $10, beater sell is done.");
                return;
            }

            NewOrderResponse orderResponse = client.newOrder(NewOrder.limitSell(Constants.PAIR, TimeInForce.GTC, sellAmount.toString(), ourAsk.toString()));
            orderId = orderResponse.getOrderId();
            log.info("Submitted GoodTillClose Limit Sell Order w/ Order ID - {}", orderId);

            log.info("Waiting 1-2 seconds before re-checking.");
            Thread.sleep(200 + new Random().nextInt(1000));

            Order order = client.getOrderStatus(new OrderStatusRequest(Constants.PAIR, orderId));

            Double fillPercentage = CommonUtil.roundUp(Double.parseDouble(order.getExecutedQty()) / Double.parseDouble(order.getOrigQty()), 4);
            while (fillPercentage < .9987) {
                log.info("Order Filled: {}%", fillPercentage * 100);
                //99% = completed logic
                Double newLowestAsk = CommonUtil.getLowestAsk(client);

                if (newLowestAsk < ourAsk) {
                    log.info("There is now an ask lower than our ask .... cancelling this order, increasing margin and resubmitting sell order");
                    CancelOrderResponse cancelOrderResponse = client.cancelOrder(new CancelOrderRequest(Constants.PAIR, orderId));
                    Double executedQuantity = Double.parseDouble(cancelOrderResponse.getExecutedQty());
                    mod++;
                    sellAmount -= executedQuantity;
                    run();
                    return;
                } else {
                    log.info("Our ask is still the lowest ... waiting 1-2 seconds before next check.");
                    Thread.sleep(200 + new Random().nextInt(1000));
                    order = client.getOrderStatus(new OrderStatusRequest(Constants.PAIR, orderId));
                    fillPercentage = CommonUtil.roundUp(Double.parseDouble(order.getExecutedQty()) / Double.parseDouble(order.getOrigQty()), 4);
                }
            }

            log.info("Beater Sell completed.");
        } catch (InterruptedException e) {
            //log.error("Interruped!!! Cancelling Sell Order and exiting...", e);
            log.info("Interrupted, cancelling order and exiting");
            this.cancelOrder();
        } finally {
            running.set(false);
        }
    }

    private Double cancelOrder() {
        if (orderId == null) return 0.0;
        CancelOrderResponse cancelOrderResponse = client.cancelOrder(new CancelOrderRequest(Constants.PAIR, orderId));
        return Double.parseDouble(cancelOrderResponse.getExecutedQty());
    }
}