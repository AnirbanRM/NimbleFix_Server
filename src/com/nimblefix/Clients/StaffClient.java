package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.OrganizationsExchangerMessage;
import com.nimblefix.ServerParam;
import com.nimblefix.core.Organization;

import javax.annotation.processing.SupportedSourceVersion;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class StaffClient {
    Socket SOCKET;
    ObjectInputStream READER;
    ObjectOutputStream WRITER;
    ServerParam serverParam;

    String userID;

    public StaffClient(Socket socket, ObjectOutputStream writer, ObjectInputStream reader, String userID, ServerParam serverParam) {
        this.SOCKET =socket;
        this.WRITER=writer;
        this.READER= reader;
        this.userID=userID;
        this.serverParam=serverParam;

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
            if(((OrganizationsExchangerMessage) object).getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_QUERY)
                exchangeOrganizationSummary((OrganizationsExchangerMessage) object);
            else if(((OrganizationsExchangerMessage) object).getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_POST)
                saveOrganization((OrganizationsExchangerMessage) object);
            else if(((OrganizationsExchangerMessage) object).getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_GET||((OrganizationsExchangerMessage) object).getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_GETALL)
                sendOrganization((OrganizationsExchangerMessage) object);
        }
    }

    private void sendOrganization(OrganizationsExchangerMessage organizationsExchangerMessage) {
        if(organizationsExchangerMessage.getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_GETALL){

            File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+organizationsExchangerMessage.getOrganizationOwner()+"/organizations");
            if(userOrganizationFileDIR.exists()){
                File orgfiles[] = userOrganizationFileDIR.listFiles();
                for(File f : orgfiles){
                    if(f.getPath().substring(f.getPath().length()-5).equals(".nfxm")) {
                        try {
                            Organization obj = (Organization) new ObjectInputStream(new FileInputStream(f)).readObject();
                            organizationsExchangerMessage.getOrganizations().add(obj);
                        }catch (Exception e){ }
                    }
                }
            }
        }
        else if(organizationsExchangerMessage.getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_GET){

            File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+organizationsExchangerMessage.getOrganizationOwner()+"/organizations");
            if(userOrganizationFileDIR.exists()){
                File orgfile = new File(userOrganizationFileDIR+"/"+organizationsExchangerMessage.getBody()+".nfxm");
                if(orgfile.exists()){
                    try {
                        Organization obj = (Organization) new ObjectInputStream(new FileInputStream(orgfile)).readObject();
                        organizationsExchangerMessage.getOrganizations().add(obj);
                    }catch (Exception e){}
                }
            }
        }

        try {
            WRITER.writeObject(organizationsExchangerMessage);
        }catch (Exception e){ }
    }

    private void saveOrganization(OrganizationsExchangerMessage organizationsExchangerMessage) {
        ArrayList<Organization> organizations = organizationsExchangerMessage.getOrganizations();

        File folder = new File(serverParam.getWorkingDirectory()+"/userdata/"+organizationsExchangerMessage.getOrganizationOwner()+"/organizations");
        if(!folder.exists())
            folder.mkdirs();

        for(Organization o : organizations) {
            try {
                FileOutputStream fos = new FileOutputStream(new File(folder.getPath() + "/" +o.getOui()+".nfxm"));
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(o);
                oos.close();
            }catch (Exception e){}
        }

        organizationsExchangerMessage.setOrganizations(null);
        organizationsExchangerMessage.setOrganizationOwner(null);
        organizationsExchangerMessage.setBody("SUCCESSFULLY SAVED");

        try {
            WRITER.writeObject(organizationsExchangerMessage);
        }catch (Exception e){ }
    }

    private void exchangeOrganizationSummary(OrganizationsExchangerMessage organizationsExchangerMessage) {

        File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+organizationsExchangerMessage.getOrganizationOwner()+"/organizations");
        if(userOrganizationFileDIR.exists()){
            File orgfiles[] = userOrganizationFileDIR.listFiles();
            for(File f : orgfiles){
                if(f.getPath().substring(f.getPath().length()-5).equals(".nfxm")) {
                    try {
                        Organization obj = (Organization) new ObjectInputStream(new FileInputStream(f)).readObject();
                        obj.setFloors(null);
                        obj.setCategories(null);
                        organizationsExchangerMessage.getOrganizations().add(obj);
                    }catch (Exception e){ }
                }
            }
        }

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
