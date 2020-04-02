package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.AboutInventoryMessage;
import com.nimblefix.ControlMessages.AccountUpdationMessage;
import com.nimblefix.ControlMessages.ComplaintMessage;
import com.nimblefix.Server;
import com.nimblefix.ServerParam;
import com.nimblefix.core.Complaint;
import com.nimblefix.core.InventoryItem;
import com.nimblefix.core.Organization;
import com.nimblefix.core.OrganizationalFloors;
import javafx.animation.ScaleTransition;
import javafx.scene.layout.Pane;

import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
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
        obj.setBody("FAILURE"); //Assume its a fail...
        String owner=null;
        ResultSet rs = Server.dbClass.executequeryView("SELECT * from organizations where orgid = '"+obj.getComplaint().getOrganizationID()+"';");
        try {
            while (rs.next())
                owner = rs.getString("owner");
        }catch (SQLException e){ }

        if(owner!=null) {
            File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory() + "/userdata/" + owner + "/organizations");
            if (userOrganizationFileDIR.exists()) {
                File orgfile = new File(userOrganizationFileDIR.getPath() + "/" + obj.getComplaint().getOrganizationID() + ".nfxm");
                if(orgfile.exists()) {
                    try {
                        FileInputStream fi = new FileInputStream(orgfile);
                        Organization o = (Organization) new ObjectInputStream(fi).readObject();
                        for (OrganizationalFloors ofl : o.getFloors()) {
                            InventoryItem i = ofl.getInventories().get(obj.getComplaint().getInventoryID());
                            if (i != null) {
                                obj = pushToDB(obj);
                                obj.getComplaint().setProblemStatus(Complaint.Status.UNFIXED);
                                obj.setBody("SUCCESS");
                            }
                        }
                        fi.close();
                    } catch (Exception e) { }
                }
            }
        }

        obj.getComplaint().setComplaintDateTime(Complaint.getDTString(new Date()));

        try {
            writer.reset();
            writer.writeUnshared(obj);
        }catch (Exception e){ }

        ComplaintMessage final_obj = obj;
        new Thread(new Runnable() {
            @Override
            public void run() {
                informComplaint(final_obj);
            }
        }).start();
    }

    private ComplaintMessage pushToDB(ComplaintMessage obj) {

        Complaint complaint = obj.getComplaint();
        long record_id = Server.dbClass.executequeryUpdateReturnID("insert into complaints(ComplaintDateTime,OrganizationID,InventoryID,UserID,UserRemarks,ProblemStatus) values('"+complaint.getComplaintDateTime()+"','"+complaint.getOrganizationID()+"','"+complaint.getInventoryID()+"','"+complaint.getUserID()+"','"+complaint.getUserRemarks()+"','UNFIXED');");
        complaint.setDbID(String.valueOf(record_id));
        return obj;
    }

    private void informComplaint(ComplaintMessage obj) {

        //Push if online
        StaffClientMonitor scm = Server.monitorStaffs.get(obj.getComplaint().getOrganizationID());
        if(scm!=null)
            scm.pushComplaint(obj.getComplaint());

        //Fetch owner from db for Organization Owner
        String owner = "";
        ResultSet rs = Server.dbClass.executequeryView("SELECT * from organizations where orgid = '"+obj.getComplaint().getOrganizationID()+"';");
        try {
            while (rs.next())
                owner = rs.getString("owner");
        }catch (SQLException e){ }

        File compDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+owner+"/complaints/"+obj.getComplaint().getOrganizationID());
        if(!compDIR.exists())
            compDIR.mkdirs();

        try {
            FileOutputStream fos = new FileOutputStream(new File(compDIR+"/"+obj.getComplaint().getComplaintID()));
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUnshared(obj.getComplaint());
            fos.close();
        } catch (Exception e) { System.out.println(e.getMessage()); }

        //Server.smtpClass.sendMail("NimbleFix Alert",,"New Complaint ("+")");
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
