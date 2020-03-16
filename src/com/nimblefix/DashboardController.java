package com.nimblefix;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class DashboardController {
    Server server;
    @FXML TextField port_box;

    public void setServer(Server s){
        this.server = s;
    }

    public void start_stop_clicked(MouseEvent mouseEvent) {
        if(!server.isListening) {
            server.startListening(port_box.getText());
        }
        else{
            server.stopListening();
        }
    }
}
