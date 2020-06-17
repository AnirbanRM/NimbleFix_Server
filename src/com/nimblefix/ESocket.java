package com.nimblefix;

import com.nimblefix.Clients.StaffClient;
import com.nimblefix.Clients.UserClient;
import com.nimblefix.Clients.WorkerClient;
import com.nimblefix.ControlMessages.AuthenticationMessage;
import com.nimblefix.core.Worker;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        if(authmsg.getSource()==AuthenticationMessage.Admin){
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
                    String otp_temp = randomOTP();
                    Server.otp_Hashmap.put(authmsg.getUser(),otp_temp);
                    sendOTPEmail(authmsg.getUser(),otp_temp);
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

        else if(authmsg.getSource()==AuthenticationMessage.Staff) {
            if(authmsg.getMessageType()==AuthenticationMessage.Response){

                //Finding owner of the Organization
                String owner = null;
                try {
                    ResultSet rs = Server.dbClass.executequeryView("SELECT * from organizations where orgid = '" + authmsg.getMESSAGEBODY() + "';");
                    while (rs.next())
                        owner = rs.getString("owner");
                }
                catch(SQLException e){}

                if(owner == null){
                    //Organization ID is Invalid!
                    AuthenticationMessage authenticationMessage = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Response,null,null);
                    authenticationMessage.setMESSAGEBODY("INVALID_ORG");
                    try {
                        WRITER.reset();
                        WRITER.writeUnshared(authenticationMessage);
                    }catch (Exception e){ clear(); }
                    clear();
                    return;
                }

                File worker_dir = new File(serverParam.getWorkingDirectory()+"/userdata/"+ owner +"/Workers/" + authmsg.getMESSAGEBODY());
                Worker worker = null;
                if(worker_dir.exists()&&worker_dir.isDirectory()) {
                    for (File f : worker_dir.listFiles()) {
                        if (f.getName().equals(authmsg.getUser())) {
                            //Getting workerfile
                            try {
                                FileInputStream fis = new FileInputStream(f);
                                ObjectInputStream ois = new ObjectInputStream(fis);
                                worker = (Worker) ois.readObject();
                                fis.close();
                            }catch (Exception e){ }
                        }
                    }

                    if(worker!=null) {

                        if(authmsg.getPassword()==null||authmsg.getPassword().length()==6) {

                            AuthenticationMessage authmsg2 = new AuthenticationMessage(AuthenticationMessage.Server, AuthenticationMessage.Response, null, null);
                            authmsg2.setMESSAGEBODY("REQUEST_OTP");
                            String otp_temp = randomOTP();
                            Server.emp_otp_Hashmap.put(worker.getEmail(), otp_temp);
                            sendOTPEmail(worker.getEmail(), otp_temp);
                            Timer t = setExpiry(authmsg.getUser());

                            try {
                                WRITER.writeUnshared(authmsg2);
                            } catch (Exception e) {
                                clear();
                                return;
                            }

                            while (true) {
                                try {
                                    authmsg2 = (AuthenticationMessage) READER.readUnshared();
                                } catch (Exception e) {
                                    clear();
                                    return;
                                }

                                if (authmsg2 != null) {
                                    if (authmsg2.getPassword().equals(Server.emp_otp_Hashmap.get(worker.getEmail()))) {
                                        t.cancel();
                                        Server.emp_otp_Hashmap.remove(worker.getEmail());
                                        handleOnCorrectOTP(authmsg2.getMESSAGEBODY(), authmsg2.getUser(),worker.getEmail());
                                        break;
                                    } else {
                                        authmsg2 = new AuthenticationMessage(AuthenticationMessage.Server, AuthenticationMessage.Response, null, null);
                                        authmsg2.setMESSAGEBODY("INVALID");
                                        try {
                                            //WRITER.reset();
                                            WRITER.writeUnshared(authmsg2);
                                            continue;
                                        } catch (Exception e) {
                                            clear();
                                        }
                                    }
                                }
                            }

                        }

                        else if(authmsg.getPassword().length()==50){
                            AuthenticationMessage authenticationMessage = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Response,null,null);
                            if(checkTokenAuthenticity(authmsg.getMESSAGEBODY(), authmsg.getUser(), authmsg.getPassword()))
                                authenticationMessage.setMESSAGEBODY("VALID");
                            else
                                authenticationMessage.setMESSAGEBODY("INVALID");
                            try {
                                WRITER.reset();
                                WRITER.writeUnshared(authenticationMessage);
                                converttoWorker(authmsg.getMESSAGEBODY(),authmsg.getUser());
                            }catch (Exception e){ clear(); }
                        }
                    }
                    else{
                        //Employee ID is Invalid!
                        AuthenticationMessage authenticationMessage = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Response,null,null);
                        authenticationMessage.setMESSAGEBODY("INVALID_EMP");
                        try {
                            WRITER.reset();
                            WRITER.writeUnshared(authenticationMessage);
                        }catch (Exception e){ clear(); }
                        clear();
                        return;
                    }
                }
            }
        }


        else{

            //TODO :: VERYY IMPORTANT

        }
    }

    private boolean checkTokenAuthenticity(String orgID, String user, String password) {
        String query = "SELECT * from worker where orgID = '"+orgID+"' and id = '"+user+"' and token = '"+password+"';";
        ResultSet rs = Server.dbClass.executequeryView(query);
        try {
            while (rs.next()){
                return true;}
            return false;
        }catch (Exception e){}
        return false;
    }

    public void sendOTPEmail(final String user, final String otp_temp){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg = "<h3>Dear User,</h3><br>"
                        +"<p>Thank you for logging into NimbleFix account.<br><br>Your OTP is : <b>"+otp_temp+"</b><br><br>"
                        +"If you didn't do it, please ignore this mail.<br><br>Thank you.</p>";
                try {
                    Server.smtpClass.sendMail("NimbleFix", user, "NimbleFix OTP", msg);
                }catch (Exception e){ System.out.println(user); System.out.println(otp_temp); System.out.println(e.getMessage().toString()); }
            }
        }).start();
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

    private void handleOnCorrectOTP(String orgID,String empID,String email) {
        AuthenticationMessage authmsg2 = new AuthenticationMessage(AuthenticationMessage.Server,AuthenticationMessage.Response,null,null);

        boolean found = false;
        String t=null;
        String query = "SELECT token from worker where orgID = '"+ orgID+"' and id = '"+empID+"';";
        ResultSet rs = Server.dbClass.executequeryView(query);
        try {
            while(rs.next()) {
                t = rs.getString("token");
                found = true;
                break;
            }
            if(!found){
                t = generateRandomToken(50);
                Server.dbClass.executequeryUpdate("INSERT INTO worker(orgID,id,token) values('"+orgID+"','"+empID+"','"+t+"');");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        authmsg2.setMESSAGEBODY("VALID"+t+email);

        try {
            WRITER.reset();
            WRITER.writeUnshared(authmsg2);
        }catch (Exception e){ clear(); }

        converttoWorker(orgID, empID);
    }

    private String generateRandomToken(int size) {
        String returnval="";
        String chars = "0123456789QWERTYUIOPLKJHGFDSAZXCVBNMqwertyuioplkjhgfdsazxcvbnm;:'.,<>/?_=+!@#$%^&*()_";
        Random r = new Random(size);
        for(int i = 0; i<size;i++){
            returnval+=chars.charAt(r.nextInt(chars.length()));
        }
        return returnval;
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
        return otp;
    }

    private boolean checkValidityStaff(String a, String b){
        String q = "SELECT * from orgadmin where email = '"+a+"' and password = '"+md5(b)+"';";
        try {
            ResultSet r = Server.dbClass.executequeryView(q);
            while (r.next()) {
                return true;
            }
            return false;
        }catch (Exception e){ return false;}
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
            }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void converttoStaff(String staffID){
        StaffClient sc = new StaffClient(SOCKET,WRITER,READER,staffID,serverParam);
    }

    private void converttoUser(String user) {
        UserClient uc = new UserClient(SOCKET,WRITER,READER,user,serverParam);
    }

    private void converttoWorker(String orgID, String empID) {
        WorkerClient wc = new WorkerClient(SOCKET,WRITER,READER,orgID,empID,serverParam);
    }

    private void clear() {
        System.out.println("Clearing client");
        try{ WRITER.close(); WRITER=null; }catch (Exception e){}
        try{ READER.close(); READER = null; }catch (Exception e){}
        try{ SOCKET.close(); SOCKET = null; }catch (Exception e){}
        try{ this.finalize(); }catch (Throwable e){}
    }
}
