package com.nimblefix.core;

import java.io.Serializable;

public class ServerConfiguration implements Serializable {

    String working_directory=null,dBName=null,dbServer=null,dBUser=null,dBPassword=null, smtp_host=null, smtp_port=null, smtp_user = null, smtp_pwd = null;
    int smtp_enc =0;

    public void setWorking_directory(String working_directory) {
        this.working_directory = working_directory;
    }

    public void setdBName(String dBName) {
        this.dBName = dBName;
    }

    public void setDbServer(String dbServer) {
        this.dbServer = dbServer;
    }

    public void setdBUser(String dBUser) {
        this.dBUser = dBUser;
    }

    public void setdBPassword(String dBPassword) {
        this.dBPassword = dBPassword;
    }

    public String getWorking_directory() {
        return working_directory;
    }

    public String getdBName() {
        return dBName;
    }

    public String getDbServer() {
        return dbServer;
    }

    public String getdBUser() {
        return dBUser;
    }

    public String getdBPassword() {
        return dBPassword;
    }

    public String getSmtp_host() {
        return smtp_host;
    }

    public String getSmtp_port() {
        return smtp_port;
    }

    public String getSmtp_user() {
        return smtp_user;
    }

    public String getSmtp_pwd() {
        return smtp_pwd;
    }

    public int getSmtp_enc() {
        return smtp_enc;
    }

    public void setSmtp_host(String smtp_host) {
        this.smtp_host = smtp_host;
    }

    public void setSmtp_port(String smtp_port) {
        this.smtp_port = smtp_port;
    }

    public void setSmtp_user(String smtp_user) {
        this.smtp_user = smtp_user;
    }

    public void setSmtp_pwd(String smtp_pwd) {
        this.smtp_pwd = smtp_pwd;
    }

    public void setSmtp_enc(int smtp_enc) {
        this.smtp_enc = smtp_enc;
    }
}
