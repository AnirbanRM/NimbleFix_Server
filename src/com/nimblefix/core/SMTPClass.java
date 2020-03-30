package com.nimblefix.core;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.Properties;

public class SMTPClass implements Serializable {

    String host, user, password,port;
    Session session;

    public class SMTPAuthentication {
        public final static int NONE = 0;
        public final static int SSL_TLS = 1;
        public final static int STARTTLS = 2;
    }

    public SMTPClass(String server, String username, String password) {
        this.host = server;
        this.user = username;
        this.password = password;

        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        session = Session.getDefaultInstance(properties,new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTPClass.this.user, SMTPClass.this.password);
            }
        });
    }

    public SMTPClass(String server, String port, String username, String password, int authentication_type) {
        this.host = server;
        this.port = port;
        this.user = username;
        this.password = password;

        if (authentication_type == SMTPAuthentication.SSL_TLS) {
            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            session = Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTPClass.this.user, SMTPClass.this.password);
                }
            });
        } else if (authentication_type == SMTPAuthentication.STARTTLS) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);

            session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTPClass.this.user, SMTPClass.this.password);
                }
            });
        }
    }

    public void sendMail(String from, String toEmail, String subject, String body) throws Exception{
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(user,from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(body,"text/html");
        Transport.send(message);
    }

    public boolean isValid() {
        try {
            sendMail(user, user, "TestMail", "This is a test mail from NimbleFix");
            return true;
        }catch (Exception e){ return false;}
    }
}