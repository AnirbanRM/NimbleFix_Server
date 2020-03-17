package com.nimblefix.ControlMessages;

import com.nimblefix.core.Organization;

import java.io.Serializable;
import java.util.ArrayList;

public class OrganizationsExchangerMessage implements Serializable {

    ArrayList<Organization> organizations;
    String organizationOwner;

    int messageType;

    public static class messageType{
        public final static int CLIENT_QUERY = 0;
        public final static int CLIENT_POST = 1;
    }

    public OrganizationsExchangerMessage(String organizationOwner,int messageType){
        this.organizationOwner = organizationOwner;
        this.messageType = messageType;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public ArrayList<Organization> getOrganizations() {
        return organizations;
    }

    public Organization getOrganization(String OUI){
        for(Organization o : this.getOrganizations())
            if(o.getOui().equals(OUI))
                return o;
        return null;
    }

    public String getOrganizationOwner() {
        return organizationOwner;
    }

    public void setOrganizationOwner(String organizationOwner) {
        this.organizationOwner = organizationOwner;
    }

    public void setOrganizations(ArrayList<Organization> organizations) {
        this.organizations = organizations;
    }

    public void addOrganization(Organization organization){
        this.organizations.add(organization);
    }

    public void deleteOrganization(Organization organization){
        this.organizations.remove(organization);
    }

    public void  deleteOrganization(int i){
        this.organizations.remove(i);
    }
}
