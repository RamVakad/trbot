package org.vakada.trbot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.vakada.trbot.common.CommonState;

public class Entry extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        CommonState.getInstance();
        CommonState.getInstance().startWorkers();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
        primaryStage.setTitle("TRBOT");
        primaryStage.setScene(new Scene(root, 1000, 600));
        primaryStage.show();
    }
}
