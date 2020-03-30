package com.nimblefix;

public class ServerParam {

    String workingDirectory;
    int portNo;
    String dBServer;
    String dBName;
    String dBUser;
    String dBPassword;
    String smtphost;
    String smtpuser;
    String smtppassword;
    String smtpport;
    int smtpauth;

    ServerParam(String workingDirectory, int dbportNo, String dBServer,String dBName,String dBUser, String dBPassword, String smtphost,String smtpuser,String smtppassword, String smtpport, int smtpauth){
        this.dBName=dBName;
        this.dBPassword=dBPassword;
        this.dBServer=dBServer;
        this.dBUser = dBUser;
        this.portNo=dbportNo;
        this.workingDirectory=workingDirectory;
        this.smtpauth = smtpauth;
        this.smtphost = smtphost;
        this.smtppassword = smtppassword;
        this.smtpport = smtpport;
        this.smtpuser = smtpuser;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public int getPortNo() {
        return portNo;
    }

    public String getdBServer() {
        return dBServer;
    }

    public String getdBName() {
        return dBName;
    }

    public String getdBUser() {
        return dBUser;
    }

    public String getdBPassword() {
        return dBPassword;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setDbPortNo(int portNo) {
        this.portNo = portNo;
    }

    public void setdBServer(String dBServer) {
        this.dBServer = dBServer;
    }

    public void setdBName(String dBName) {
        this.dBName = dBName;
    }

    public void setdBUser(String dBUser) {
        this.dBUser = dBUser;
    }

    public void setdBPassword(String dBPassword) {
        this.dBPassword = dBPassword;
    }

    public String getSmtphost() {
        return smtphost;
    }

    public String getSmtpuser() {
        return smtpuser;
    }

    public String getSmtppassword() {
        return smtppassword;
    }

    public String getSmtpport() {
        return smtpport;
    }

    public int getSmtpauth() {
        return smtpauth;
    }

    public void setPortNo(int portNo) {
        this.portNo = portNo;
    }

    public void setSmtphost(String smtphost) {
        this.smtphost = smtphost;
    }

    public void setSmtpuser(String smtpuser) {
        this.smtpuser = smtpuser;
    }

    public void setSmtppassword(String smtppassword) {
        this.smtppassword = smtppassword;
    }

    public void setSmtpport(String smtpport) {
        this.smtpport = smtpport;
    }

    public void setSmtpauth(int smtpauth) {
        this.smtpauth = smtpauth;
    }
}
