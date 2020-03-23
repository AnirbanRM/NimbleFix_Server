package com.nimblefix.core;

import java.sql.*;

public class DBClass {

    final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    String DB_SERVER = "jdbc:mysql://localhost/";
    String DB_NAME = "BinderDB";
    String USER = "BinderClient";
    String PASS = "1234";

    Connection con=null;

    DBClass(String host, String dB, String user, String password){
        this.DB_SERVER = "jdbc:mysql://"+host+"/";
        this.DB_NAME = dB;
        this.USER = user;
        this.PASS = password;

        try {Class.forName(JDBC_DRIVER);}catch(Exception e) { System.out.println(e.getMessage());}
        try {
            con = DriverManager.getConnection(DB_SERVER+DB_NAME ,USER,PASS);
        }catch(SQLException e) { System.out.println("UNABLE TO CONENCT TO DATABASE ! Error : " + e.getMessage());}
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
