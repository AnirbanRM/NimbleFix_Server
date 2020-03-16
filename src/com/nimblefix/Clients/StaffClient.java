package com.nimblefix.Clients;

import com.nimblefix.ControlMessages.OrganizationsExchangerMessage;
import com.sun.org.apache.regexp.internal.RE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class StaffClient {
    Socket SOCKET;
    ObjectInputStream READER;
    ObjectOutputStream WRITER;

    public StaffClient(Socket socket, ObjectOutputStream writer, ObjectInputStream reader) {
        this.SOCKET =socket;
        this.WRITER=writer;
        this.READER= reader;

        listentoIncomingObjects();
    }

    private void listentoIncomingObjects(){
        Thread reader_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Object obj = READER.readObject();

                        System.out.println(obj.getClass());



                    } catch (Exception e) { e.printStackTrace();}
                }
            }
        });
        reader_thread.start();
    }
}
