package com.nimblefix;

import com.nimblefix.Clients.StaffClient;
import com.nimblefix.Clients.UserClient;
import com.nimblefix.ControlMessages.AuthenticationMessage;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.Time;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

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
            WRITER.writeUnshared(authmsg);
            Object reply_obj = READER.readUnshared();
            handleAuthentication((AuthenticationMessage)reply_obj);
        } catch (Exception e) { clear(); e.printStackTrace(); }
    }

    private  void handleAuthentication(AuthenticationMessage authmsg){
        if(authmsg.getSource()==AuthenticationMessage.Staff){
            if(authmsg.getMessageType()==AuthenticationMessage.Response){
                AuthenticationMessage authmsg2 = new AuthenticationMessage(AuthenticationMessage.Server, AuthenticationMessage.Response, null, null);
                if(checkValidityStaff(authmsg.getUser(),authmsg.getPassword())) authmsg2.setMESSAGEBODY("SUCCESS");
                else authmsg2.setMESSAGEBODY("FAILURE");
                try{WRITER.writeUnshared(authmsg2);}catch (Exception e){ }

                if(authmsg2.getMESSAGEBODY().equals("FAILURE"))
                    clear();
                else if(authmsg2.getMESSAGEBODY().equals("SUCCESS"))
                    converttoStaff(authmsg.getUser());
            }
        }

        else if(authmsg.getSource()==AuthenticationMessage.User) {
            if(authmsg.getMessageType()==AuthenticationMessage.Response){

                if(authmsg.getPassword()==null||authmsg.getPassword().length()==6){

                    AuthenticationMessage authmsg2 = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Response,null,null);
                    authmsg2.setMESSAGEBODY("REQUEST_OTP");
                    Server.otp_Hashmap.put(authmsg.getUser(),randomOTP());
                    Timer t = setExpiry(authmsg.getUser());

                    try {
                        WRITER.writeUnshared(authmsg2);
                    } catch (Exception e) {
                        clear();
                        return;
                    }

                    while(true){
                        try {
                            authmsg2 = (AuthenticationMessage) READER.readUnshared();
                        }catch (Exception e){
                            clear();
                            return;
                        }

                        if(authmsg2!=null){
                            if(authmsg2.getPassword().equals(Server.otp_Hashmap.get(authmsg2.getUser()))){
                                t.cancel();
                                Server.otp_Hashmap.remove(authmsg2.getUser());
                                handleOnCorrectOTP(authmsg2.getUser());
                                break;
                            }
                            else{
                                authmsg2 = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Response,null,null);
                                authmsg2.setMESSAGEBODY("INVALID");
                                try {
                                    //WRITER.reset();
                                    WRITER.writeUnshared(authmsg2);
                                    continue;
                                }catch (Exception e){ clear(); }
                            }
                        }
                    }
                }
                else if(authmsg.getPassword().length()==50){
                    AuthenticationMessage authenticationMessage = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Response,null,null);
                    if(checkTokenAuthenticity(authmsg.getUser(),authmsg.getPassword()))
                        authenticationMessage.setMESSAGEBODY("VALID");
                    else
                        authenticationMessage.setMESSAGEBODY("INVALID");
                    try {
                        WRITER.reset();
                        WRITER.writeUnshared(authenticationMessage);
                        converttoUser(authmsg.getUser());
                    }catch (Exception e){ clear(); }
                }
            }
        }

        else{

            //TODO :: VERYY IMPORTANT

        }
    }

    private boolean checkTokenAuthenticity(String user, String password) {
        String query = "SELECT * from client where email = '"+user+"' and token = '"+password+"';";
        ResultSet rs = Server.dbClass.executequeryView(query);
        try {
            while (rs.next()){
                return true;}
            return false;
        }catch (Exception e){}
        return false;
    }

    private void handleOnCorrectOTP(String user) {
        AuthenticationMessage authmsg2 = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Response,null,null);
        authmsg2.setMESSAGEBODY("VALID");

        try {
            WRITER.reset();
            WRITER.writeUnshared(authmsg2);
        }catch (Exception e){ clear(); }

        converttoUser(user);
    }

    private Timer setExpiry(final String user) {
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                Server.otp_Hashmap.remove(user);
            }
        },5*60*1000 );
        return t;
    }

    private String randomOTP() {
        String otp = "";
        Random rand = new Random();
        for(int i = 0; i< 6; i++)
            otp+=String.valueOf(rand.nextInt(10));
        System.out.println(otp);
        return otp;
    }

    private boolean checkValidityStaff(String a, String b){
        if(a.equals("Anirban")&&b.equals("123456"))
            return true;
        else return false;
    }

    private void converttoStaff(String staffID){
        StaffClient sc = new StaffClient(SOCKET,WRITER,READER,staffID,serverParam);
    }

    private void converttoUser(String user) {
        UserClient uc = new UserClient(SOCKET,WRITER,READER,user,serverParam);

    }

    private void clear() {
        System.out.println("Clearing client");
        try{ WRITER.close(); WRITER=null; }catch (Exception e){}
        try{ READER.close(); READER = null; }catch (Exception e){}
        try{ SOCKET.close(); SOCKET = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }
}
