package com.loosenotes.model;

public class User {
    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String role;
    private String createdAt;
    private int noteCount; // transient – populated for admin views

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getNoteCount() { return noteCount; }
    public void setNoteCount(int noteCount) { this.noteCount = noteCount; }
}
