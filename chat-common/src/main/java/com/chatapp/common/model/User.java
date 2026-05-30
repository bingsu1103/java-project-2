package com.chatapp.common.model;

import java.io.Serializable;

/**
 * Represents a chat user.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;  // Only used during registration/login, not sent in chat messages
    private boolean online;

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.online = false;
    }

    // --- Getters & Setters ---

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return username + (online ? " (online)" : " (offline)");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username != null && username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }
}
