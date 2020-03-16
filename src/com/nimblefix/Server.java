package com.nimblefix;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {
    boolean isListening = false;
    final int DEF_PORT_NO = 2180;

    Thread connection_acceptor_thread;
    ServerSocket serverSocket;


    //HashMaps for different sockets
    //===================
    //===================

    //TODO To make different user classes and add hasmap


    //===================
    //===================



    Server(){
        System.out.println("Server constructed");
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
                                    ESocket s = new ESocket(socket);
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
