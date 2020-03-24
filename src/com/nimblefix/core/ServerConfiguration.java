package com.nimblefix.core;

import java.io.Serializable;

public class ServerConfiguration implements Serializable {

    String working_directory=null,dBName=null,dbServer=null,dBUser=null,dBPassword=null;

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
}
