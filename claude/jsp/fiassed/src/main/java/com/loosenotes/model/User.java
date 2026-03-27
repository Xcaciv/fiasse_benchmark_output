package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * User domain model.
 * Password hash stored; plaintext never retained (F-01, Confidentiality).
 */
public class User {

    public enum Role {
        USER, ADMIN
    }

    private Long id;
    private String username;
    private String email;
    private String passwordHash;  // BCrypt hash only; never plaintext
    private Role role;
    private int failedLoginAttempts;
    private LocalDateTime lockoutUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
        this.role = Role.USER;
        this.failedLoginAttempts = 0;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(LocalDateTime lockoutUntil) { this.lockoutUntil = lockoutUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** Convenience: is this user currently locked out? */
    public boolean isLockedOut() {
        return lockoutUntil != null && LocalDateTime.now().isBefore(lockoutUntil);
    }

    /** Convenience: is this user an admin? */
    public boolean isAdmin() {
        return Role.ADMIN.equals(role);
    }
}
