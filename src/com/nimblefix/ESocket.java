package com.nimblefix;

import com.nimblefix.Clients.StaffClient;
import com.nimblefix.ControlMessages.AuthenticationMessage;

import java.io.*;
import java.net.Socket;

public class ESocket {
    Socket SOCKET=null;
    ObjectInputStream READER = null;
    ObjectOutputStream WRITER = null;

    ServerParam serverParam;

    ESocket(Socket s,ServerParam serverParam) throws IOException {
        this.serverParam=serverParam;
        System.out.println("Accepted from "+s.getInetAddress());
        this.SOCKET = s;
        try {
            WRITER = new ObjectOutputStream(s.getOutputStream());
            READER = new ObjectInputStream(s.getInputStream());
        }catch(Exception e){System.out.println(e.toString()); }
        progress();
    }
    public void progress(){
        Thread new_thd = new Thread(new Runnable() {
            @Override
            public void run() {
                ESocket.this.Authenticate();
            }
        });
        new_thd.start();
    }

    private void Authenticate() {
        AuthenticationMessage authmsg = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Challenge,null,null);
        try {
            WRITER.writeObject(authmsg);
            Object reply_obj = READER.readObject();
            handleAuthentication((AuthenticationMessage)reply_obj);
        } catch (Exception e) { clear(); e.printStackTrace(); }
    }

    private  void handleAuthentication(AuthenticationMessage authmsg){
        if(authmsg.getSource()==AuthenticationMessage.Staff){
            if(authmsg.getMessageType()==AuthenticationMessage.Response){
                AuthenticationMessage authmsg2 = new AuthenticationMessage(AuthenticationMessage.Server, AuthenticationMessage.Response, null, null);
                if(checkValidityStaff(authmsg.getUser(),authmsg.getPassword())) authmsg2.setMESSAGEBODY("SUCCESS");
                else authmsg2.setMESSAGEBODY("FAILURE");
                try{WRITER.writeObject(authmsg2);}catch (Exception e){ }

                if(authmsg2.getMESSAGEBODY().equals("FAILURE"))
                    clear();
                else if(authmsg2.getMESSAGEBODY().equals("SUCCESS"))
                    converttoStaff(authmsg.getUser());
            }
        }
        else{

            //TODO :: VERYY IMPORTANT

        }
    }

    private boolean checkValidityStaff(String a, String b){
        if(a.equals("Anirban")&&b.equals("123456"))
            return true;
        else return false;
    }

    private void converttoStaff(String staffID){
        StaffClient sc = new StaffClient(SOCKET,WRITER,READER,staffID,serverParam);
    }

    private void clear() {
        System.out.println("Clearing client");
        try{ WRITER.close(); WRITER=null; }catch (Exception e){}
        try{ READER.close(); READER = null; }catch (Exception e){}
        try{ SOCKET.close(); SOCKET = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }
}
