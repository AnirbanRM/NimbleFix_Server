package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.AboutInventoryMessage;
import com.nimblefix.ControlMessages.ComplaintMessage;
import com.nimblefix.ControlMessages.WorkerExchangeMessage;
import com.nimblefix.Server;
import com.nimblefix.ServerParam;
import com.nimblefix.core.*;

import java.io.*;
import java.net.Socket;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public class WorkerClient {

    Socket socket;
    ObjectInputStream reader;
    ObjectOutputStream writer;

    String orgID,empID;

    ServerParam serverParam;

    boolean listen = true;

    public WorkerClient(Socket socket, ObjectOutputStream writer, ObjectInputStream reader, String orgID, String empID, ServerParam serverParam) {
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        this.orgID = orgID;
        this.empID = empID;
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
        if(obj instanceof ComplaintMessage){
            handleComplaintMessage((ComplaintMessage)obj);
        }
        if(obj instanceof AboutInventoryMessage){
            sendAboutInventory((AboutInventoryMessage)obj);
        }

    }

    private void handleComplaintMessage(ComplaintMessage msg) {
        if(msg.getBody().contains("FETCH")){
            String temp = msg.getBody().split(":")[1];
            String orgID = temp.split("/")[0];
            String ID = temp.split("/")[1];
            String owner=null;

            String query = "SELECT * from complaints,organizations where complaints.OrganizationID = '"+ orgID+"' and complaints.AssignedTo = '"+ID+"' and complaints.OrganizationID = organizations.orgid and complaints.ProblemStatus = 'UNFIXED';";
            System.out.println(query);
            ResultSet rs = Server.dbClass.executequeryView(query);
            ArrayList<Complaint> complaints = new ArrayList<Complaint>();
            try {
                while (rs.next()) {
                    Complaint c = new Complaint(rs.getString("OrganizationID"),rs.getString("InventoryID"),null,null,null);
                    c.setAssignedDate(rs.getString("AssignedDateTime"));
                    c.setDbID(String.valueOf(rs.getInt("ID")));
                    complaints.add(c);
                    owner = rs.getString("owner");
                }
            }catch (SQLException e){System.out.println(e.toString());}

            ComplaintMessage cmsg = new ComplaintMessage(complaints);
            cmsg.setBody(owner);

            try{
                writer.reset();
                writer.writeUnshared(cmsg);
            }catch (Exception e){}
        }

        else if(msg.getBody().contains("IMAGE")){
            File compFile = new File(serverParam.getWorkingDirectory()+"/userdata/"+ msg.getBody().substring(5) +"/complaints/"+orgID + "/" + msg.getComplaint().getComplaintID());
            Complaint c=null;
            try {
                FileInputStream fis = new FileInputStream(compFile);
                ObjectInputStream oos = new ObjectInputStream(fis);
                c = (Complaint) oos.readObject();
                fis.close();
            } catch (Exception e) { System.out.println(e.getMessage()); }
            try{
                ComplaintMessage cmsg = new ComplaintMessage(c);
                writer.writeUnshared(cmsg);
            }catch (Exception e){}
        }

        else if(msg.getBody().contains("DONE")){
            setAsDone(msg.getBody().substring(4), msg.getComplaint());
            try {
                writer.writeUnshared(new ComplaintMessage((Complaint) null));
            }catch (Exception e){}
        }
    }

    private void setAsDone(String owner, Complaint complaint) {
        String query = "update complaints set ProblemStatus = 'FIXED', fixedDateTime = '"+ Complaint.getDTString(new Date())+"' where ID = "+complaint.getDbID()+";";
        Server.dbClass.executequeryUpdate(query);
        File compFile = new File(serverParam.getWorkingDirectory()+"/userdata/"+ owner +"/complaints/"+ complaint.getOrganizationID() + "/" + complaint.getComplaintID());
        compFile.delete();
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

    private void clear() {
        System.out.println("Clearing Workerclient");
        try{ writer.close(); writer=null; }catch (Exception e){}
        try{ reader.close(); reader = null; }catch (Exception e){}
        try{ socket.close(); socket = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }

}
