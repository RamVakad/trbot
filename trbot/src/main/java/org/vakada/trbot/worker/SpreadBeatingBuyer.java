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
public class SpreadBeatingBuyer implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private Double buyAmount;
    private BinanceApiRestClient client;
    private Integer mod;
    private Long orderId;

    public SpreadBeatingBuyer(Double buyAmount) {
        this.buyAmount = buyAmount;
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
            log.info("Performing Beater Buy, amount: {}, mod: {}", buyAmount, mod);

            if (buyAmount > 0 && buyAmount < 10) {
                log.info("Buy Amount is < $10, beater buy is done.");
                return;
            }

            Double usdtBalance = Double.parseDouble(client.getAccount().getAssetBalance("USDT").getFree());

            if (buyAmount > usdtBalance) {
                log.error("Buy Amount is greater than USDT Balance in account .... failing out");
                return;
            }

            if (buyAmount == -1.0) {
                buyAmount = usdtBalance;
            }

            buyAmount = Math.min(buyAmount, Constants.MAX_USDT);

            log.info("Buying Power: ${}", buyAmount);

            Double highestBid = CommonUtil.getHighestBid(client);

            log.info("Current Highest Bid for {}: ${}", Constants.PAIR, highestBid);
            Double randInc = CommonUtil.getSpreadBeaterAmount(mod);
            log.info("Calculated Spread Beater: ${}", randInc);
            Double ourBid = CommonUtil.roundDown(highestBid + randInc, 3);

            log.info("Setting our bid to ${}", ourBid);

            Double quantity = CommonUtil.roundDown(buyAmount / ourBid, 2);

            log.info("Setting our quantity to: {}", quantity);

            NewOrderResponse orderResponse = client.newOrder(NewOrder.limitBuy(Constants.PAIR, TimeInForce.GTC, quantity.toString(), ourBid.toString()));
            orderId = orderResponse.getOrderId();
            log.info("Submitted GoodTillClose Limit Buy Order w/ Order ID - {}", orderId);

            log.info("Waiting 1-2 seconds before re-checking.");
            Thread.sleep(200 + new Random().nextInt(1000));

            Order order = client.getOrderStatus(new OrderStatusRequest(Constants.PAIR, orderId));
            Double fillPercentage = CommonUtil.roundUp(Double.parseDouble(order.getExecutedQty()) / Double.parseDouble(order.getOrigQty()), 4);
            while (fillPercentage < .9987) {
                log.info("Order Filled: {}%", fillPercentage * 100);
                //99% = completed logic
                Double newHighestBid = CommonUtil.getHighestBid(client);

                if (newHighestBid > ourBid) {
                    log.info("There is now a bid higher than our bid .... cancelling this order, increasing margin and resubmitting buy order");
                    Double executedQuantity = this.cancelOrder();
                    mod++;
                    buyAmount -= (executedQuantity * ourBid);
                    run();
                    return;
                } else {
                    log.info("Our bid is still the highest ... waiting 1-2 seconds before next check.");
                    Thread.sleep(200 + new Random().nextInt(1000));
                    order = client.getOrderStatus(new OrderStatusRequest(Constants.PAIR, orderId));
                    fillPercentage = CommonUtil.roundUp(Double.parseDouble(order.getExecutedQty()) / Double.parseDouble(order.getOrigQty()), 4);
                }
            }

            log.info("Beater buy completed.");
        } catch (InterruptedException e) {
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