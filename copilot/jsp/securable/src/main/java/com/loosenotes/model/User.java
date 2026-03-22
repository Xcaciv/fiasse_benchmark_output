package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Immutable domain model for application users.
 * password_hash is never surfaced to views (Confidentiality).
 */
public final class User {

    private final long id;
    private final String username;
    private final String email;
    /** Never exposed outside service layer */
    private final String passwordHash;
    private final String role;
    private final LocalDateTime createdAt;

    public User(long id, String username, String email,
                String passwordHash, String role, LocalDateTime createdAt) {
        this.id           = id;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.createdAt    = createdAt;
    }

    public long getId()            { return id; }
    public String getUsername()    { return username; }
    public String getEmail()       { return email; }
    public String getPasswordHash(){ return passwordHash; }
    public String getRole()        { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        // Intentionally excludes passwordHash (Confidentiality)
        return "User{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}
