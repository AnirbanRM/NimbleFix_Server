package com.nimblefix;

import com.nimblefix.Clients.StaffClientMonitor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    boolean isListening = false;
    final int DEF_PORT_NO = 2180;

    Thread connection_acceptor_thread;
    ServerSocket serverSocket;
    ServerParam serverParam;

    //HashMaps for different sockets
    public static ConcurrentHashMap<String, StaffClientMonitor> monitorStaffs;
    //===================

    //TODO To make different user classes and add hasmap


    //===================
    //===================



    Server(ServerParam serverParam){
        this.serverParam=serverParam;

        // TODO : Initialize all the user HashMaps
        monitorStaffs = new ConcurrentHashMap<String, StaffClientMonitor>();
        //TODO:-----------------------------------


        System.out.println("Server constructed");
    }

    public ServerParam getServerParam() {
        return serverParam;
    }

    public void setServerParam(ServerParam serverParam) {
        this.serverParam = serverParam;
    }

    public void startListening(String p){
        isListening=true;
        connection_acceptor_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Starting server");
                int port;
                try { port = Integer.parseInt(p); }
                catch (Exception e){ port = DEF_PORT_NO; }

                try {
                    serverSocket = new ServerSocket(port);
                    }
                 catch (Exception e) { }

                while(true) {
                    try {
                        Socket socket = serverSocket.accept();
                        Thread newClient = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ESocket s = new ESocket(socket,serverParam);
                                } catch (IOException e) { e.printStackTrace(); }
                            }
                        });
                        newClient.start();

                    } catch (Exception e2) { if(e2.getMessage().equals("socket closed"))System.out.println("Server Stopped"); }

                }
            }
        });
        connection_acceptor_thread.start();
    }

    public void stopListening() {
        isListening=false;
        try{serverSocket.close();
            System.out.println("Stopping server");
        }
        catch (Exception e){}
    }
}
