package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Represents an application user.
 * SSEM: Confidentiality - password_hash is never exposed via toString().
 * SSEM: Integrity - immutable ID, validation constraints enforced at DB level.
 */
public class User {

    private long id;
    private String username;
    private String email;
    /** Never expose in logs, API responses, or views. */
    private String passwordHash;
    private Role role;
    private LocalDateTime createdAt;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;

    public User() {
        this.role = Role.USER;
        this.failedLoginAttempts = 0;
    }

    // ---- Getters ----

    public long getId() { return id; }

    public String getUsername() { return username; }

    public String getEmail() { return email; }

    /** Returns the BCrypt hash. Never log or expose this value. */
    public String getPasswordHash() { return passwordHash; }

    public Role getRole() { return role; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }

    public boolean isAdmin() { return Role.ADMIN.equals(role); }

    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    // ---- Setters ----

    public void setId(long id) { this.id = id; }

    public void setUsername(String username) { this.username = username; }

    public void setEmail(String email) { this.email = email; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void setRole(Role role) { this.role = role; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    /**
     * SSEM: Confidentiality - never include password hash in toString output.
     */
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + "}";
    }
}
