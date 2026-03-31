package com.loosenotes.model;

import java.time.Instant;

/**
 * Represents an application user.
 *
 * SSEM notes:
 * - Confidentiality: passwordHash is not exposed via toString(); no cleartext password field.
 * - Analyzability: plain data-holder, no logic.
 * - Modifiability: Role is an enum, not a free string.
 */
public class User {

    private long id;
    private String username;
    private String email;
    /** BCrypt hash – never the raw password. */
    private String passwordHash;
    private Role role;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAdmin() { return Role.ADMIN.equals(role); }

    /** Excludes passwordHash intentionally. */
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + '}';
    }
}
