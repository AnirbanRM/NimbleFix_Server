package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.ComplaintMessage;
import com.nimblefix.ControlMessages.PendingWorkMessage;
import com.nimblefix.ControlMessages.WorkerExchangeMessage;
import com.nimblefix.Server;
import com.nimblefix.ServerParam;
import com.nimblefix.core.*;

import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StaffClientMonitor {

    Socket SOCKET;
    ObjectInputStream READER;
    ObjectOutputStream WRITER;
    ServerParam serverParam;

    String userID;
    Organization organization;

    public StaffClientMonitor(StaffClient staffClient, Organization organization) {
        System.out.println("Converting StaffClient to Staff ClientMonitor");
        this.SOCKET = staffClient.SOCKET;
        this.WRITER = staffClient.WRITER;
        this.READER = staffClient.READER;
        this.userID = staffClient.userID;
        this.serverParam = staffClient.serverParam;
        this.organization = organization;

        Server.monitorStaffs.put(organization.getOui(),this);

        listentoIncomingObjects();
    }

    private void writeObject(final Object obj){
        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WRITER.reset();
                    WRITER.writeUnshared(obj);
                }catch (Exception e){ clear();}
            }
        });
        writer.start();
    }

    private void listentoIncomingObjects(){
        Thread reader_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Object obj = READER.readUnshared();
                        handle(obj);
                    } catch (Exception e) { clear(); break;}
                }
            }
        });
        reader_thread.start();
    }

    public void pushComplaint(Complaint complaint){
        try {
            WRITER.reset();
            WRITER.writeUnshared(new ComplaintMessage(complaint));
        }catch (Exception e){ e.printStackTrace(); }
    }

    private void handle(Object object){
        if(object instanceof ComplaintMessage) {
            if(((ComplaintMessage) object).getBody().substring(0,3).equals("GET"))
                sendPastComplaints((ComplaintMessage)object);
            else if(((ComplaintMessage) object).getBody().equals("ASSIGNMENT"))
                handleAssignment((ComplaintMessage) object);
        }
        else if(object instanceof WorkerExchangeMessage){
            if(((WorkerExchangeMessage)object).getBody().equals("FETCH"))
                pushWorkerInfo((WorkerExchangeMessage) object);
        }
        else if(object instanceof PendingWorkMessage){
            if(((PendingWorkMessage)object).getBody().equals("FETCH"))
                pushPendingWorkData((PendingWorkMessage) object);
        }
            //TODO : Handle different objects

    }

    private void pushPendingWorkData(PendingWorkMessage pendingWorkMessage) {
        ResultSet rs = Server.dbClass.executequeryView("select COUNT(AssignedTo) as Count,AssignedTo from complaints where OrganizationID = '"+pendingWorkMessage.getOrganizationID()+"' and ProblemStatus = 'UNFIXED';");
        Map<String,Integer> map = new HashMap<String,Integer>();

        try {
            while (rs.next()) {
                map.put(rs.getString("AssignedTo"),Integer.parseInt(rs.getString("Count")));
            }
        }catch (SQLException e){ }

        pendingWorkMessage.setPendingTasks(map);
        pendingWorkMessage.setBody("RESULT");

        try {
            WRITER.reset();
            WRITER.writeUnshared(pendingWorkMessage);
        } catch (Exception e) { }
    }

    private void sendPastComplaints(ComplaintMessage object) {
        File compDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+userID+"/complaints/"+organization.getOui()+"/");
        if(compDIR.exists()){
            File orgfiles[] = compDIR.listFiles();
            for(File f : orgfiles){
                    try {
                        FileInputStream fi = new FileInputStream(f);
                        Complaint comp = (Complaint) new ObjectInputStream(fi).readObject();
                        object.getComplaints().add(comp);
                        fi.close();
                    }catch (Exception e){ System.out.println(e.getMessage().toString()); }
                }
            }

        try {
            WRITER.reset();
            WRITER.writeUnshared(object);
        } catch (Exception e) { }
    }

    private void handleAssignment(ComplaintMessage complaint) {
        Server.dbClass.executequeryUpdate("update complaints set AssignedBy = '"+complaint.getComplaint().getAssignedBy()+"', AssignedTo = '"+complaint.getComplaint().getAssignedTo()+"', AssignedDateTime = '"+complaint.getComplaint().getAssignedDateTime()+"', AdminComments = '"+complaint.getComplaint().getAdminComments()+"' where ID = "+complaint.getComplaint().getDbID()+";");
        sendEmailforAssignment(complaint);
        updateComplaintinFS(complaint);
        complaint.setBody("ASSIGNMENT_SUCCESS");

        try {
            WRITER.reset();
            WRITER.writeUnshared(complaint);
        }catch (Exception e){ e.printStackTrace(); }
    }

    private void updateComplaintinFS(ComplaintMessage complaint) {
        File compFile = new File(serverParam.getWorkingDirectory()+"/userdata/"+ userID +"/complaints/"+complaint.getComplaint().getOrganizationID() + "/" + complaint.getComplaint().getComplaintID());
        compFile.delete();

        try {
            FileOutputStream fos = new FileOutputStream(compFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUnshared(complaint.getComplaint());
            fos.close();
        } catch (Exception e) { System.out.println(e.getMessage()); }
    }

    private void sendEmailforAssignment(ComplaintMessage complaint){
        new Thread(() -> {
            try {
                String body = SMTPClass.getWorkNotificationMessage(complaint.getInventoryItem() , complaint.getFloorID() ,complaint.getComplaint());
                Server.smtpClass.sendMail(complaint.getComplaint().getAssignedBy(), complaint.getComplaint().getAssignedTo(), "New Assignment", body,complaint.getLocation_image());
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void clear() {
        System.out.println("Clearing Monitoring client");
        Server.monitorStaffs.remove(organization.getOui());

        try{ WRITER.close(); WRITER=null; }catch (Exception e){}
        try{ READER.close(); READER = null; }catch (Exception e){}
        try{ SOCKET.close(); SOCKET = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }

    private void pushWorkerInfo(WorkerExchangeMessage object) {
        ArrayList<Worker> workers = new ArrayList<>();
        WorkerExchangeMessage wem = new WorkerExchangeMessage(null,null, workers);
        wem.setBody("FETCHRESULT");

        File dir = new File(serverParam.getWorkingDirectory()+"/userdata/"+ userID+ "/Workers/" + object.getOrganizationID());
        if(dir.exists()){
            File[] files = dir.listFiles();
            for(File i : files){
                try {
                    FileInputStream fis = new FileInputStream(i);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    Worker w = (Worker) ois.readObject();
                    if(w!=null)
                        wem.getWorkers().add(w);
                    fis.close();
                }catch (Exception e){ }
            }
        }

        try{
            WRITER.reset();
            WRITER.writeUnshared(wem);
        }catch (Exception e){ }
    }
}
