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
                        Object obj = READER.readUnshared();
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
            else if(((OrganizationsExchangerMessage) object).getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_DELETE)
                deleteOrganization((OrganizationsExchangerMessage) object);
        }
    }

    private void deleteOrganization(OrganizationsExchangerMessage organizationsExchangerMessage) {
        if(organizationsExchangerMessage.getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_DELETE){
            File f = new File(serverParam.getWorkingDirectory()+"/userdata/"+organizationsExchangerMessage.getOrganizationOwner()+"/organizations/"+organizationsExchangerMessage.getBody()+".nfxm");
            if(f.exists())
                f.delete();

            organizationsExchangerMessage.setBody("SUCCESS");
            organizationsExchangerMessage.setOrganizationOwner(null);
            try {
                WRITER.writeUnshared(organizationsExchangerMessage);
            }catch (Exception e){}
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
                            FileInputStream fi = new FileInputStream(f);
                            Organization obj = (Organization) new ObjectInputStream(fi).readObject();
                            organizationsExchangerMessage.getOrganizations().add(obj);
                            fi.close();
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
                        FileInputStream fi = new FileInputStream(orgfile);
                        Organization obj = (Organization) new ObjectInputStream(fi).readObject();
                        organizationsExchangerMessage.getOrganizations().add(obj);
                        fi.close();
                    }catch (Exception e){}
                }
            }
        }

        try {
            WRITER.writeUnshared(organizationsExchangerMessage);
        }catch (Exception e){ }
    }

    private void saveOrganization(OrganizationsExchangerMessage organizationsExchangerMessage) {
        ArrayList<Organization> organizations = organizationsExchangerMessage.getOrganizations();

        File folder = new File(serverParam.getWorkingDirectory()+"/userdata/"+organizationsExchangerMessage.getOrganizationOwner()+"/organizations");
        if(!folder.exists())
            folder.mkdirs();

        for(Organization o : organizations) {
            try {
                File f = new File(folder.getPath() + "/" +o.getOui()+".nfxm");
                if(f.exists())f.delete();

                FileOutputStream fos = new FileOutputStream(f);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(o);
                fos.close();
            }catch (Exception e){}
        }

        organizationsExchangerMessage.setOrganizations(null);
        organizationsExchangerMessage.setOrganizationOwner(null);
        organizationsExchangerMessage.setBody("SUCCESSFULLY SAVED");

        try {
            WRITER.writeUnshared(organizationsExchangerMessage);
        }catch (Exception e){ }
    }

    private void exchangeOrganizationSummary(OrganizationsExchangerMessage organizationsExchangerMessage) {

        File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+organizationsExchangerMessage.getOrganizationOwner()+"/organizations");
        if(userOrganizationFileDIR.exists()){
            File orgfiles[] = userOrganizationFileDIR.listFiles();
            for(File f : orgfiles){
                if(f.getPath().substring(f.getPath().length()-5).equals(".nfxm")) {
                    try {
                        FileInputStream fi= new FileInputStream(f);
                        Organization obj = (Organization) new ObjectInputStream(fi).readObject();
                        obj.setFloors(null);
                        obj.setCategories(null);
                        organizationsExchangerMessage.getOrganizations().add(obj);
                        fi.close();
                    }catch (Exception e){ }
                }
            }
        }

        try {
            WRITER.writeUnshared(organizationsExchangerMessage);
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
