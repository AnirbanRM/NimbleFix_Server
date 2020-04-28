package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.*;
import com.nimblefix.Server;
import com.nimblefix.ServerParam;
import com.nimblefix.core.*;
import com.sun.corba.se.spi.orbutil.threadpool.Work;
import com.sun.istack.internal.Nullable;
import javafx.application.Platform;
import javafx.scene.layout.Pane;

import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StaffClient {
    Socket SOCKET;
    ObjectInputStream READER;
    ObjectOutputStream WRITER;
    ServerParam serverParam;

    String userID;

    boolean listen=true;

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
                    if(!listen)break;
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
        else if(object instanceof MonitorMessage){
            handleMonitor((MonitorMessage) object);
        }
        else if(object instanceof WorkerExchangeMessage){
            if(((WorkerExchangeMessage)object).getBody().equals("UPDATE"))
                handleWorkerUpdate((WorkerExchangeMessage)object);
            else if(((WorkerExchangeMessage)object).getBody().equals("FETCH"))
                pushWorkerInfo((WorkerExchangeMessage)object);
        }
        else if(object instanceof MaintainenceMessage){
            handleMaintainenceMessage((MaintainenceMessage)object);
        }
        else if(object instanceof HistoryMessage){
            handleHistoryMessage((HistoryMessage)object);
        }

    }

    private void handleHistoryMessage(HistoryMessage message) {
        if(message.getBody().equals("FETCH")) {
            if(message.getBody()!=null) {
                HistoryMessage newMsg = new HistoryMessage(message.getOui());
                ResultSet resultSet = Server.dbClass.executequeryView("SELECT * from complaints where OrganizationID = '"+ message.getOui() +"';");
                try {

                    while (resultSet.next()){
                        InventoryItemHistory currentItemHistory = new InventoryItemHistory(resultSet.getString("OrganizationID"),resultSet.getString("InventoryID"));
                        currentItemHistory.setEventID(resultSet.getString("ID"));
                        currentItemHistory.setAssignedTo(resultSet.getString("AssignedTo"));

                        currentItemHistory.setWorkDateTime(resultSet.getString("fixedDateTime"));
                        currentItemHistory.setEventType(InventoryItemHistory.Type.FIXED);
                        if(currentItemHistory.getWorkDateTime()==null) {
                            currentItemHistory.setWorkDateTime(resultSet.getString("assignedDateTime"));
                            currentItemHistory.setEventType(InventoryItemHistory.Type.ASSIGNED);
                            if(currentItemHistory.getWorkDateTime()==null) {
                                currentItemHistory.setWorkDateTime(resultSet.getString("complaintDateTime"));
                                currentItemHistory.setEventType(InventoryItemHistory.Type.REGISTERED);
                            }
                        }

                        newMsg.addHistory(resultSet.getString("InventoryID"),currentItemHistory);
                    }

                }catch (Exception e){ }

                newMsg.setBody("RESULT");

                try{
                    WRITER.reset();
                    WRITER.writeUnshared(newMsg);
                }catch (Exception e){}
            }
        }
    }

    private void handleMaintainenceMessage(MaintainenceMessage message) {
        if(message.getBody().equals("CONFIG")){
            File f = new File(serverParam.getWorkingDirectory()+"/userdata/"+ userID+ "/Maintainence");
            if(!f.exists()) f.mkdirs();
            f = new File(f.getPath()+"/"+message.getOui());
            if(f.exists())f.delete();
            message.setBody("SUCCESS");
            try{
                FileOutputStream fos = new FileOutputStream(f);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeUnshared(message.getMaintainenceMap());
                fos.close();
            }catch (Exception e){ message.setBody("FAILURE"); }
            message.setMaintainenceMap(null);

            try {
                WRITER.reset();
                WRITER.writeUnshared(message);
            }catch (Exception e){ }
        }

        else if(message.getBody().equals("FETCH")){
            File f = new File(serverParam.getWorkingDirectory()+"/userdata/"+ userID+ "/Maintainence/"+message.getOui());
            message.setBody("FETCHRESULT");
            if(f.exists()){
                try{
                    FileInputStream fis = new FileInputStream(f);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    Object o = ois.readObject();
                    fis.close();
                    message.setMaintainenceMap((Map<String, InventoryMaintainenceClass>) o);
                }catch (Exception e){ }
            }
            try {
                WRITER.reset();
                WRITER.writeUnshared(message);
            }catch (Exception e){ }
        }
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

    private void handleWorkerUpdate(WorkerExchangeMessage wem) {
        WorkerExchangeMessage workerExchangeMessage = new WorkerExchangeMessage(null,null,null);

        File dir = new File(serverParam.getWorkingDirectory()+"/userdata/"+ userID+"/Workers/");
        File workerDir = new File(dir.getPath()+"/"+wem.getOrganizationID());
        if(!workerDir.exists())
            workerDir.mkdirs();

        for(File i : workerDir.listFiles())
            i.delete();

        workerExchangeMessage.setBody("SUCCESS");
        try {
            for(Worker w : wem.getWorkers()){
                File workerfile = new File(workerDir.getPath() +"/"+w.getEmpID());
                FileOutputStream fos = new FileOutputStream(workerfile);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeUnshared(w);
                fos.close();
            }
        }catch (Exception e){
            workerExchangeMessage.setBody("FAILURE");
            System.out.println(e.getMessage());
        }

        try {
            WRITER.reset();
            WRITER.writeUnshared(workerExchangeMessage);
        }catch (Exception e){ }
    }

    private void deleteOrganization(OrganizationsExchangerMessage organizationsExchangerMessage) {
        if(organizationsExchangerMessage.getMessageType()==OrganizationsExchangerMessage.messageType.CLIENT_DELETE){

            Server.dbClass.executequeryUpdate("DELETE from organizations where orgid = '"+organizationsExchangerMessage.getBody()+"';");

            File f = new File(serverParam.getWorkingDirectory()+"/userdata/"+userID+"/organizations/"+organizationsExchangerMessage.getBody()+".nfxm");
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

            File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+userID+"/organizations");
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

            File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+userID+"/organizations");
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

        File folder = new File(serverParam.getWorkingDirectory()+"/userdata/"+userID+"/organizations");
        if(!folder.exists())
            folder.mkdirs();

        for(Organization o : organizations) {

            try {
                ResultSet rs = Server.dbClass.executequeryView("SELECT * from organizations where orgid = '" + o.getOui() + "';");
                while(rs.next()){
                    if(rs.getString("owner").equals(userID))break;
                    organizationsExchangerMessage.setOrganizations(null);
                    organizationsExchangerMessage.setOrganizationOwner(null);
                    organizationsExchangerMessage.setBody("EXISTS");
                    WRITER.writeUnshared(organizationsExchangerMessage);
                    return;
                }
                Server.dbClass.executequeryUpdate("INSERT INTO organizations(orgid,owner) values('"+o.getOui()+"','"+userID+"');");

            }catch (Exception e){ }


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

        File userOrganizationFileDIR = new File(serverParam.getWorkingDirectory()+"/userdata/"+userID+"/organizations");
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



    private void handleMonitor(MonitorMessage object) {
        Organization o =null;

        if(object.getAdminID().equals(this.userID)&&object.getMessageType()==MonitorMessage.MessageType.CLIENT_MONITOR_START){

            File f = new File(serverParam.getWorkingDirectory()+"/userdata/"+object.getAdminID()+"/organizations/"+ object.getOrganizationID()+".nfxm");
            if(f.exists()){
                try {
                    FileInputStream fis = new FileInputStream(f);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    o = (Organization) ois.readObject();
                    object.setOrganization(o);
                    object.setOrganizationID(o.getOui());
                    object.setBody("SUCCESS");
                    fis.close();
                }catch(Exception e){ object.setBody("CORRUPTED_FILE"); }
            }
            else object.setBody("FILE_NOT_FOUND");

            object.setAdminID(null);
            try {
                WRITER.writeUnshared(object);
                if(o!=null) {
                    listen=false;
                    StaffClientMonitor scm = new StaffClientMonitor(this, o);
                    this.finalize();
                }
            } catch(Exception e){ } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private void clear() {
        System.out.println("Clearing client");
        try{ WRITER.close(); WRITER=null; }catch (Exception e){}
        try{ READER.close(); READER = null; }catch (Exception e){}
        try{ SOCKET.close(); SOCKET = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }
}
