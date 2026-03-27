package com.loosenotes.model;

/**
 * Represents an application user.
 * Role is an enum to prevent injection of arbitrary role strings.
 */
public class User {

    public enum Role {
        USER, ADMIN
    }

    private long id;
    private String username;
    private String email;
    private String passwordHash;
    private Role role;
    private String resetToken;
    private Long resetTokenExpiry;
    private long createdAt;

    public User() {
        this.role = Role.USER;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /** Never log or expose passwordHash. */
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    /** Single-use reset token — cleared after use. */
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public Long getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(Long resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }
}
