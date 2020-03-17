package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.OrganizationsExchangerMessage;
import com.nimblefix.core.Organization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class StaffClient {
    Socket SOCKET;
    ObjectInputStream READER;
    ObjectOutputStream WRITER;

    String userID;

    public StaffClient(Socket socket, ObjectOutputStream writer, ObjectInputStream reader,String userID) {
        this.SOCKET =socket;
        this.WRITER=writer;
        this.READER= reader;
        this.userID=userID;

        listentoIncomingObjects();
    }

    private void listentoIncomingObjects(){
        Thread reader_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Object obj = READER.readObject();
                        handle(obj);
                    } catch (Exception e) { clear(); break;}
        }
            }
        });
        reader_thread.start();
    }

    private void handle(Object object){
        if(object instanceof OrganizationsExchangerMessage){
            exchangeOrganization((OrganizationsExchangerMessage) object);
        }
    }

    private void exchangeOrganization(OrganizationsExchangerMessage organizationsExchangerMessage) {
        ArrayList<Organization> organizations = new ArrayList<Organization>();

        organizations.add(new Organization("Bennett University"));
        organizations.add(new Organization("IGI Airport"));
        organizations.add(new Organization("Grand Venice Mall"));

        organizationsExchangerMessage.setOrganizations(organizations);

        try {
            WRITER.writeObject(organizationsExchangerMessage);
        } catch (IOException e) { System.out.println(e.getMessage()); }

    }

    private void clear() {
        System.out.println("Clearing client");
        try{ WRITER.close(); WRITER=null; }catch (Exception e){}
        try{ READER.close(); READER = null; }catch (Exception e){}
        try{ SOCKET.close(); SOCKET = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }
}
