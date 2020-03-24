package com.nimblefix.core;

import java.sql.*;

public class DBClass {

    final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    String DB_SERVER = "";
    String DB_NAME = "";
    String USER = "";
    String PASS = "";

    Connection con=null;

    boolean isConfigurationValid = false;

    public DBClass(String host, String dB, String user, String password){
        this.DB_SERVER = "jdbc:mysql://"+host+"/";
        this.DB_NAME = dB;
        this.USER = user;
        this.PASS = password;

        try {Class.forName(JDBC_DRIVER);}catch(Exception e) { System.out.println(e.getMessage());}
        try {
            con = DriverManager.getConnection(DB_SERVER+DB_NAME ,USER,PASS);
            isConfigurationValid=true;
        }catch(SQLException e) { System.out.println("UNABLE TO CONENCT TO DATABASE ! Error : " + e.getMessage());}
    }

    public boolean isConfigurationValid() {
        return isConfigurationValid;
    }

    public ResultSet executequeryView(String query) {
        Statement stmt;
        ResultSet result = null;
        try {
            stmt = con.createStatement();
            result = stmt.executeQuery(query);
        }catch(Exception e) { System.out.println(e.getMessage()); }
        return result;
    }

    public void executequeryUpdate(String query) {
        Statement stmt;
        try {
            stmt = con.createStatement();
            stmt.executeUpdate(query);
        }catch(Exception e) { System.out.println(e.getMessage()); }
    }

}
