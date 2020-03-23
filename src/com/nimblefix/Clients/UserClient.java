package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.AboutInventoryMessage;
import com.nimblefix.ControlMessages.AccountUpdationMessage;
import com.nimblefix.ControlMessages.ComplaintMessage;
import com.nimblefix.ServerParam;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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

    }

    private void handleComplaint(ComplaintMessage obj) {

    }

    private void updateAccount(AccountUpdationMessage obj) {

    }

    private void clear() {
        System.out.println("Clearing client");
        try{ writer.close(); writer=null; }catch (Exception e){}
        try{ reader.close(); reader = null; }catch (Exception e){}
        try{ socket.close(); socket = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }

}
