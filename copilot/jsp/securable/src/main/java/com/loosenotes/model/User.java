package com.loosenotes.model;

public class User {
    private final long id;
    private final String username;
    private final String email;
    private final String role;
    private final String passwordHash;
    private final String passwordSalt;
    private final String createdAt;

    public User(long id, String username, String email, String role, String passwordHash, String passwordSalt, String createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }
    public String getCreatedAt() { return createdAt; }
    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
}
