package org.vakada.trbot.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.vakada.trbot.common.CommonState;
import org.vakada.trbot.common.Constants;
import org.vakada.trbot.util.CommonUtil;
import org.vakada.trbot.worker.SpreadBeatingBuyer;
import org.vakada.trbot.worker.SpreadBeatingSeller;

import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class MainController {

    @FXML
    public Label altField;

    @FXML
    public TextField buyAmount;

    @FXML
    public TextField sellAmount;

    @FXML
    private Label priceField;

    @FXML
    private Label lowestAskField;

    @FXML
    private Label highestBidField;

    @FXML
    private TextArea textArea;

    private SpreadBeatingBuyer buyer;

    private SpreadBeatingSeller seller;


    @FXML
    public void initialize() {
        startThread(); // Perfectly Ok here, as FXMLLoader already populated all @FXML annotated members.
    }

    private void startThread() {
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            Double currPrice = CommonState.getInstance().getPrice();
                            setPrice(currPrice.toString());
                            setLowestAskField(CommonState.getInstance().getLowestAsk().toString());
                            setHighestBidField(CommonState.getInstance().getHighestBid().toString());
                            setAltField("Spread: $" + CommonUtil.roundDown(CommonState.getInstance().getLowestAsk() - CommonState.getInstance().getHighestBid(), 2));

                            Double totalUsdtBalance = Double.parseDouble(CommonState.getInstance().getUsdtBalance().getFree()) + Double.parseDouble(CommonState.getInstance().getUsdtBalance().getLocked());
                            Double totalSymbol = Double.parseDouble(CommonState.getInstance().getPairBalance().getFree()) + Double.parseDouble(CommonState.getInstance().getPairBalance().getLocked());
                            Double returnPercent = CommonUtil.roundUp((CommonState.getInstance().getHighestBid() - CommonState.getInstance().getLastBuy()) / CommonState.getInstance().getLastBuy(), 4) * 100;
                            String text = "USDT Balance: $" + CommonUtil.roundDown(totalUsdtBalance, 2) + "\n" +
                                    Constants.SYMBOL + " Balance: " + CommonUtil.roundDown(totalSymbol, 2) + "\n" +
                                    "Total Value: $" + CommonUtil.roundDown((totalUsdtBalance + (totalSymbol * currPrice)), 2) + "\n\n" +
                                    "Last Buy Price: $" + CommonState.getInstance().getLastBuy() + "\n" +
                                    "Return: " + returnPercent + "%" + (returnPercent > 10 ? " - TAKE PROFIT!!" : "");
                            setTextArea(text);
                        });
                    }
                }, 4000, 200);
    }

    public void setPrice(String price) {
        Platform.runLater(() -> priceField.setText("$" + price));
    }

    public void setLowestAskField(String price) {
        Platform.runLater(() -> lowestAskField.setText("$" + price));
    }

    public void setHighestBidField(String price) {
        Platform.runLater(() -> highestBidField.setText("$" + price));
    }

    public void setAltField(String msg) {
        Platform.runLater(() -> altField.setText(msg));
    }

    public void setTextArea(String text) {
        Platform.runLater(() -> textArea.setText(text));
    }

    public void buyClicked(ActionEvent actionEvent) {
        log.info("BUY CLICKED.");
        new Timer().schedule(
                new TimerTask() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        if (buyer != null && buyer.isRunning()) {
                            buyer.stop();
                        }
                        while (buyer != null && buyer.isRunning()) {
                            Thread.sleep(500);
                        }
                        if (buyAmount.getText().equalsIgnoreCase("STOP")) {
                            return;
                        }
                        buyer = new SpreadBeatingBuyer(Double.parseDouble(buyAmount.getText()));
                        buyer.start();
                    }
                }, 0);
    }

    public void sellClicked(ActionEvent actionEvent) {
        log.info("SELL CLICKED.");
        new Timer().schedule(
                new TimerTask() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        if (seller != null && seller.isRunning()) {
                            seller.stop();
                        }
                        while (seller != null && seller.isRunning()) {
                            Thread.sleep(500);
                        }
                        if (sellAmount.getText().equalsIgnoreCase("STOP")) {
                            return;
                        }
                        seller = new SpreadBeatingSeller(Double.parseDouble(sellAmount.getText()));
                        seller.start();
                    }
                }, 0);
    }
}
