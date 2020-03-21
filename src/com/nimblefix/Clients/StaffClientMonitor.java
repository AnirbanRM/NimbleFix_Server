package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.ComplaintMessage;
import com.nimblefix.Server;
import com.nimblefix.ServerParam;
import com.nimblefix.core.Complaint;
import com.nimblefix.core.Organization;

import java.io.*;
import java.net.Socket;

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

    private void handle(Object object){
        if(object instanceof ComplaintMessage){
            handleComplaint((Complaint)((ComplaintMessage) object).getComplaint());

            //TODO : Handle different objects



        }
    }

    private void handleComplaint(Complaint complaint) {

        StaffClientMonitor scm = Server.monitorStaffs.get(complaint.getOrganizationID());
        if(scm==null){
            System.out.println("Staff not online");
            return;
        }

        try {
            scm.WRITER.writeUnshared(new ComplaintMessage(complaint));
        }catch (Exception e){ e.printStackTrace(); }
    }

    private void clear() {
        System.out.println("Clearing Monitoring client");
        Server.monitorStaffs.remove(organization.getOui());

        try{ WRITER.close(); WRITER=null; }catch (Exception e){}
        try{ READER.close(); READER = null; }catch (Exception e){}
        try{ SOCKET.close(); SOCKET = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }
}
