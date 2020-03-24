package com.nimblefix;

import com.nimblefix.core.DBClass;
import com.nimblefix.core.ServerConfiguration;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {
    public Stage curr_stg;
    Server server;
    @FXML TextField port_box,dBServer,dBName,dBPassword,dBUser;
    @FXML ImageView server_status;
    @FXML Label server_status_l,wd_label;

    Image tick = new Image("file://"+ getClass().getResource("/resources/tick.png").getPath(), 80, 80, true, true);
    Image cross = new Image("file://"+ getClass().getResource("/resources/cross.png").getPath(), 80, 80, true, true);

    public void start_stop_clicked(MouseEvent mouseEvent) {
        if(!server.isListening) {
            server.setServerParam(new ServerParam(wd_label.getText(),Integer.parseInt(port_box.getText()),dBServer.getText() ,dBName.getText(), dBUser.getText(), dBPassword.getText()));
            server.startListening(port_box.getText());
            server_status.setImage(tick);
            server_status_l.setText("Server running");

        }
        else{
            server.stopListening();
            server_status.setImage(cross);
            server_status_l.setText("Server not running");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        server_status.setImage(cross);
        server = new Server(null);

        loadConfiguration();

        wd_label.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setWorking_directory(newValue);});
        dBServer.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setDbServer(newValue);});
        dBName.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setdBName(newValue);});
        dBUser.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setdBUser(newValue);});
        dBPassword.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setdBPassword(newValue);});
    }

    public void setSaveConfig(){
        curr_stg.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                File config = new File(System.getenv("PROGRAMDATA")+"/NimbleFix/config" +"/settings.dat");
                try {
                    FileOutputStream fo = new FileOutputStream(config);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fo);
                    objectOutputStream.writeObject(Server.currentConfiguration);
                    fo.close();
                }catch (Exception e) { }
            }
        });
    }

    private void loadConfiguration() {
        if(Server.currentConfiguration.getWorking_directory()!=null)
            wd_label.setText(Server.currentConfiguration.getWorking_directory());

        if(Server.currentConfiguration.getdBName()!=null)
            dBName.setText(Server.currentConfiguration.getdBName());

        if(Server.currentConfiguration.getDbServer()!=null)
            dBServer.setText(Server.currentConfiguration.getDbServer());

        if(Server.currentConfiguration.getdBUser()!=null)
            dBUser.setText(Server.currentConfiguration.getdBUser());

        if(Server.currentConfiguration.getdBPassword()!=null)
            dBPassword.setText(Server.currentConfiguration.getdBPassword());
    }

    public void browse_clicked(MouseEvent mouseEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select working directory");
        File temp = dc.showDialog(curr_stg);
        if(temp!=null)
            wd_label.setText(temp.getPath());
    }

    public void checkdBConnActivity(MouseEvent mouseEvent) {
        DBClass test = new DBClass(dBServer.getText() ,dBName.getText(), dBUser.getText(), dBPassword.getText());
        if(test.isConfigurationValid()){
            Alert a = new Alert(Alert.AlertType.INFORMATION ,null, ButtonType.OK);
            a.setHeaderText("Connection to Database successful.");
            a.setTitle("Success");
            a.showAndWait();
        }else{
            Alert a = new Alert(Alert.AlertType.ERROR ,null, ButtonType.OK);
            a.setHeaderText("Connection to Database unsuccessful.");
            a.setTitle("Error");
            a.showAndWait();
        }
        test = null;
    }
}