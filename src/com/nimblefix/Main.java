package com.nimblefix;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("DashboardUI.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Dashboard");
        primaryStage.setScene(new Scene(root, 500, 600));
        primaryStage.setResizable(false);
        primaryStage.show();
        ((DashboardController)loader.getController()).curr_stg = primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}