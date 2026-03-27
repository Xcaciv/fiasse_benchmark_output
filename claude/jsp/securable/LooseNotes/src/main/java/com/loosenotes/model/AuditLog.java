package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Represents an audit log entry.
 * SSEM: Accountability - structured security event record.
 * Never populate details with passwords, tokens, or session IDs.
 */
public class AuditLog {

    private long id;
    /** Nullable - actions before auth or by deleted users have no userId. */
    private Long userId;
    private String action;
    private String resourceType;
    private Long resourceId;
    /** Human-readable context. Must not contain sensitive data. */
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLog() {}

    // ---- Getters ----

    public long getId() { return id; }

    public Long getUserId() { return userId; }

    public String getAction() { return action; }

    public String getResourceType() { return resourceType; }

    public Long getResourceId() { return resourceId; }

    public String getDetails() { return details; }

    public String getIpAddress() { return ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    // ---- Setters ----

    public void setId(long id) { this.id = id; }

    public void setUserId(Long userId) { this.userId = userId; }

    public void setAction(String action) { this.action = action; }

    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public void setDetails(String details) { this.details = details; }

    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
