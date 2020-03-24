package com.nimblefix.ControlMessages;

import java.io.Serializable;

public class AccountUpdationMessage implements Serializable {

    String email;
    String name;
    String dp;

    String token;

    public AccountUpdationMessage(String email, String name, String dp) {
        this.email = email;
        this.name = name;
        this.dp = dp;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getDp() {
        return dp;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDp(String dp) {
        this.dp = dp;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
