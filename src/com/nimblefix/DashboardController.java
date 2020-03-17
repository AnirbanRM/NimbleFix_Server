package com.nimblefix;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {
    public Stage curr_stg;
    Server server;
    @FXML TextField port_box;
    @FXML ImageView server_status;
    @FXML Label server_status_l,wd_label;

    Image tick = new Image("file://"+ getClass().getResource("/resources/tick.png").getPath(), 80, 80, true, true);
    Image cross = new Image("file://"+ getClass().getResource("/resources/cross.png").getPath(), 80, 80, true, true);

    public void start_stop_clicked(MouseEvent mouseEvent) {
        if(!server.isListening) {
            server.setServerParam(new ServerParam(wd_label.getText(),Integer.parseInt(port_box.getText()),null,null, null, null ));
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
    }

    public void browse_clicked(MouseEvent mouseEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select working directory");
        File temp = dc.showDialog(curr_stg);
        if(temp!=null)
            wd_label.setText(temp.getPath());
    }
}