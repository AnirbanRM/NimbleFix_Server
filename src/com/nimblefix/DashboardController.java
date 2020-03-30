package com.nimblefix;

import com.nimblefix.core.DBClass;
import com.nimblefix.core.SMTPClass;
import com.nimblefix.core.ServerConfiguration;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

import static com.nimblefix.Server.smtpClass;

public class DashboardController implements Initializable {
    public Stage curr_stg;
    Server server;
    @FXML TextField port_box,dBServer,dBName,dBPassword,dBUser, smtp_server_box,smtp_port_box,smtp_user,smtp_pwd;
    @FXML ImageView server_status;
    @FXML Label server_status_l,wd_label;
    @FXML RadioButton enc_n,enc_s,enc_st;
    @FXML ToggleGroup g1;

    Image tick = new Image("file://"+ getClass().getResource("/resources/tick.png").getPath(), 80, 80, true, true);
    Image cross = new Image("file://"+ getClass().getResource("/resources/cross.png").getPath(), 80, 80, true, true);

    public void start_stop_clicked(MouseEvent mouseEvent) {
        if(!server.isListening) {
            ServerParam param = new ServerParam(wd_label.getText(),Integer.parseInt(port_box.getText()),dBServer.getText() ,dBName.getText(), dBUser.getText(), dBPassword.getText(), smtp_server_box.getText(),smtp_user.getText(),smtp_pwd.getText(),smtp_port_box.getText(), SMTPClass.SMTPAuthentication.NONE);
            if(enc_s.isSelected())param.setSmtpauth(SMTPClass.SMTPAuthentication.SSL_TLS);
            if(enc_st.isSelected())param.setSmtpauth(SMTPClass.SMTPAuthentication.STARTTLS);
            server.setServerParam(param);
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

        smtp_server_box.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setSmtp_host(newValue);});
        smtp_port_box.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setSmtp_port(newValue);});
        smtp_user.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setSmtp_user(newValue);});
        smtp_pwd.textProperty().addListener((observable, oldValue, newValue) -> { Server.currentConfiguration.setSmtp_pwd(newValue);});

        g1.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
           if(enc_n.isSelected())  Server.currentConfiguration.setSmtp_enc(SMTPClass.SMTPAuthentication.NONE);
           if(enc_s.isSelected())  Server.currentConfiguration.setSmtp_enc(SMTPClass.SMTPAuthentication.SSL_TLS);
           if(enc_st.isSelected())  Server.currentConfiguration.setSmtp_enc(SMTPClass.SMTPAuthentication.STARTTLS);
        });
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

        if(Server.currentConfiguration.getSmtp_host()!=null)
            smtp_server_box.setText(Server.currentConfiguration.getSmtp_host());

        if(Server.currentConfiguration.getSmtp_port()!=null)
            smtp_port_box.setText(Server.currentConfiguration.getSmtp_port());

        if(Server.currentConfiguration.getSmtp_user()!=null)
            smtp_user.setText(Server.currentConfiguration.getSmtp_user());

        if(Server.currentConfiguration.getSmtp_pwd()!=null)
            smtp_pwd.setText(Server.currentConfiguration.getSmtp_pwd());

        ((Server.currentConfiguration.getSmtp_enc()==0)?enc_n:(Server.currentConfiguration.getSmtp_enc()==1?enc_s:enc_st)).setSelected(true);
    }

    public void browse_clicked(MouseEvent mouseEvent) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select working directory");
        File temp = dc.showDialog(curr_stg);
        if(temp!=null)
            wd_label.setText(temp.getPath());
    }

    public void checkdBConnectivity(MouseEvent mouseEvent) {
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
    }
    public void checkSMTPConnectivity(MouseEvent mouseEvent){
        Alert a = new Alert(Alert.AlertType.INFORMATION ,null, new ButtonType(""));
        a.setHeaderText("Trying to validate...");
        a.setTitle("Please Wait");
        a.show();

        boolean valid = false;
        SMTPClass smtpClass;
        if(enc_n.isSelected())
            smtpClass = new SMTPClass(smtp_server_box.getText(),smtp_user.getText(),smtp_pwd.getText());
        else
            smtpClass = new SMTPClass(smtp_server_box.getText(),smtp_port_box.getText(), smtp_user.getText(),smtp_pwd.getText(),enc_s.isSelected()? SMTPClass.SMTPAuthentication.SSL_TLS: SMTPClass.SMTPAuthentication.STARTTLS);

        valid = smtpClass.isValid();

        a.close();

        if(valid){
            a = new Alert(Alert.AlertType.INFORMATION ,null, ButtonType.OK);
            a.setHeaderText("Configuration Valid");
            a.setTitle("Success");
            a.showAndWait();
        }
        else{
            a = new Alert(Alert.AlertType.ERROR ,null, ButtonType.OK);
            a.setHeaderText("Configuration Invalid");
            a.setTitle("Error");
            a.showAndWait();
        }
    }
}