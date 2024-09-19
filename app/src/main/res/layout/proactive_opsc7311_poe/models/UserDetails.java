package com.example.proactive_opsc7311_poe.models;

import java.util.Date;

public class UserDetails {
    private String UID;
    private String firstname;
    private String lastname;
    private String pfp;
    private Date dob;
    private String gender;

    public UserDetails(String UID, String firstname, String lastname, String pfp, Date dob, String gender) {
        this.UID = UID;
        this.firstname = firstname;
        this.lastname = lastname;
        this.pfp = pfp;
        this.dob = dob;
        this.gender = gender;
    }

    public String getUID() {
        return UID;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPfp() {
        return pfp;
    }

    public Date getDob() {
        return dob;
    }

    public String getGender() {
        return gender;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setPfp(String pfp) {
        this.pfp = pfp;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
