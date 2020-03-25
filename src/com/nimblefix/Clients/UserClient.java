package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.AboutInventoryMessage;
import com.nimblefix.ControlMessages.AccountUpdationMessage;
import com.nimblefix.ControlMessages.ComplaintMessage;
import com.nimblefix.Server;
import com.nimblefix.ServerParam;
import com.nimblefix.core.InventoryItem;
import com.nimblefix.core.Organization;
import com.nimblefix.core.OrganizationalFloors;
import javafx.animation.ScaleTransition;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class UserClient {

    String emailID;

    Socket socket;
    ObjectOutputStream writer;
    ObjectInputStream reader;

    ServerParam serverParam;
    boolean listen = true;

    public UserClient(Socket socket, ObjectOutputStream writer, ObjectInputStream reader, String userID, ServerParam serverParam) {
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.emailID = userID;
        this.serverParam = serverParam;

        startListening();
    }

    private void startListening() {
        Thread reader_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(!listen)break;
                    try {
                        Object obj = reader.readUnshared();
                        handle(obj);
                    } catch (Exception e) { clear(); break;}
                }
            }
        });
        reader_thread.start();
    }

    private void handle(Object obj) {

        if(obj instanceof AccountUpdationMessage)
            updateAccount((AccountUpdationMessage)obj);
        else if(obj instanceof ComplaintMessage)
            handleComplaint((ComplaintMessage) obj);
        else if(obj instanceof AboutInventoryMessage)
            sendAboutInventory((AboutInventoryMessage)obj);
    }

    private void sendAboutInventory(AboutInventoryMessage obj) {
        String owner = "";
        ResultSet rs = Server.dbClass.executequeryView("SELECT * from organizations where orgid = '"+obj.getOui()+"';");
        try {
            while (rs.next())
                owner = rs.getString("owner");
        }catch (SQLException e){ }

        File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+owner+"/organizations");
        if(userOrganizationFileDIR.exists()){
            File orgfiles = new File(userOrganizationFileDIR.getPath()+"/"+obj.getOui()+".nfxm");
            try {
                FileInputStream fi = new FileInputStream(orgfiles);
                Organization o = (Organization) new ObjectInputStream(fi).readObject();
                for(OrganizationalFloors ofl : o.getFloors()){
                    InventoryItem i = ofl.getInventories().get(obj.getId());
                    if(i!=null) {
                        obj.setTitle(i.getTitle());
                        obj.setDescription(i.getDescription());
                        obj.setOrg(i.getParentOrganization().getOrganization_Name());
                    }
                }
                fi.close();
            }catch (Exception e){ }
        }

        try {
            writer.reset();
            writer.writeUnshared(obj);
        }catch (Exception e){ }
    }

    private void handleComplaint(ComplaintMessage obj) {

    }

    private void updateAccount(AccountUpdationMessage obj) {
        String token = writeToDBandgetToken(emailID,obj.getName(),obj.getDp());
        obj.setToken(token);
        try {
            writer.reset();
            writer.writeUnshared(obj);
        }catch (Exception e){ }
    }

    private String writeToDBandgetToken(String emailID, String name, String dp) {

        String query = "SELECT token from client where email = '"+ emailID+"';";
        ResultSet rs = Server.dbClass.executequeryView(query);
        try {
            while(rs.next()){
                return rs.getString("token");
            }

            String t = generateRandomToken(50);
            Server.dbClass.executequeryUpdate("INSERT INTO client(name,email,DP,token) values('"+name+"','"+emailID+"','"+dp+"','"+t+"');");
            return t;
        } catch (SQLException e) {
            e.printStackTrace(); return null;
        }
    }

    private String generateRandomToken(int size) {
        String returnval="";
        String chars = "0123456789QWERTYUIOPLKJHGFDSAZXCVBNMqwertyuioplkjhgfdsazxcvbnm;:'.,<>/?_=+!@#$%^&*()_";
        Random r = new Random(size);
        for(int i = 0; i<size;i++){
            returnval+=chars.charAt(r.nextInt(chars.length()));
        }
        return returnval;
    }

    private void clear() {
        System.out.println("Clearing Userclient");
        try{ writer.close(); writer=null; }catch (Exception e){}
        try{ reader.close(); reader = null; }catch (Exception e){}
        try{ socket.close(); socket = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }

}
