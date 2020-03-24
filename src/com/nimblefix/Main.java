package com.nimblefix;

import com.nimblefix.core.ServerConfiguration;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("DashboardUI.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Dashboard");
        primaryStage.setScene(new Scene(root, 1020, 520));
        primaryStage.setResizable(false);
        primaryStage.show();
        ((DashboardController)loader.getController()).curr_stg = primaryStage;
        ((DashboardController)loader.getController()).setSaveConfig();

    }

    public static void main(String[] args) {

        //Create Server Configuration File
        File f = new File(System.getenv("PROGRAMDATA")+"/NimbleFix/config");
        if(!f.exists())
            f.mkdirs();

        File config = new File(f.getPath()+"/settings.dat");
        if(!config.exists()){
            try {
                FileOutputStream fo = new FileOutputStream(config);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fo);
                objectOutputStream.writeObject(new ServerConfiguration());
                fo.close();
            }catch (Exception e) { }
        }

        launch(args);
    }
}