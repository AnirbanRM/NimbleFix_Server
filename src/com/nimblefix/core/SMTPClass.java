package com.nimblefix.core;

import java.io.Serializable;

public class SMTPClass implements Serializable {

    String server = "";
    int port;
    int authentication;

    public Class SMTPAuthentication{
        public final static int NONE = 1;
        public final static int SSL_TLS = 2;
        public final static int STARTTLS = 3;
    }

    public SMTPClass(String server,int port, int authentication){
        this.authentication = authentication;
        this.port = port;
        this.server = server;
    }

}
