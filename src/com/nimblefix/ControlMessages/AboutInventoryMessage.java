package com.nimblefix.ControlMessages;

import java.io.Serializable;

public class AboutInventoryMessage implements Serializable {

    String oui;
    String id;

    String title;
    String description;
    String org;

    AboutInventoryMessage(String oui_id){
        String[] t = oui_id.split("/");
        oui = t[0].trim();
        id = t[1].trim();
    }

    public AboutInventoryMessage(String oui, String id){
        this.oui = oui;
        this.id = id;
    }

    public String getOui() {
        return oui;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setOui(String oui) {
        this.oui = oui;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }
}