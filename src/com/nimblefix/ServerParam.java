package com.nimblefix;

public class ServerParam {

    String workingDirectory;
    int portNo;
    String dBServer;
    String dBName;
    String dBUser;
    String dBPassword;

    ServerParam(String workingDirectory, int portNo, String dBServer,String dBName,String dBUser, String dBPassword){
        this.dBName=dBName;
        this.dBPassword=dBPassword;
        this.dBServer=dBServer;
        this.dBUser = dBUser;
        this.portNo=portNo;
        this.workingDirectory=workingDirectory;
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

    public void setPortNo(int portNo) {
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
}
