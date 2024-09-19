package com.example.proactive_opsc7311_poe.models;

public class User {
    private String UID;
    private String email;
    private String password;

    public User(String UID, String email, String password) {
        this.UID = UID;
        this.email = email;
        this.password = password;
    }

    public String getUID() {
        return UID;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
