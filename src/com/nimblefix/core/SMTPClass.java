package com.nimblefix.core;


import com.sun.corba.se.pept.transport.TransportManager;
import org.simplejavamail.mailer.MailerBuilder;

import java.io.Serializable;

public class SMTPClass implements Serializable {

    String server = "";
    int port;
    int authentication;

    public class SMTPAuthentication{
        public final static int NONE = 1;
        public final static int SSL_TLS = 2;
        public final static int STARTTLS = 3;
    }

    public SMTPClass(String server,int port, int authentication){
        this.authentication = authentication;
        this.port = port;
        this.server = server;
    }

    public void connect(){
        
        /*String mailer = MailerBuilder
                .withSMTPServer("smtp.host.com", 587, "user@host.com", "password")
                .withTransportStrategy(Transport)
                .withProxy("socksproxy.host.com", 1080, "proxy user", "proxy password")
                .withSessionTimeout(10 * 1000)
                .clearEmailAddressCriteria() // turns off email validation
                .withProperty("mail.smtp.sendpartial", true)
                .withDebugLogging(true)
                .async()
                .buildMailer();
*/


    }

}
