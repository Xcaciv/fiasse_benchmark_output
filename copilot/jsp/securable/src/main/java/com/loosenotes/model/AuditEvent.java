package com.loosenotes.model;

/**
 * Immutable representation of a security-relevant audit event.
 * No PII (password, email) is stored in audit events — only identifiers and actions.
 */
public class AuditEvent {

    private long id;
    private String eventType;
    private Long userId;
    private String username;
    private String resourceType;
    private String resourceId;
    private String ipAddress;
    private long eventTimestamp;
    private String details;

    public AuditEvent() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public long getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(long eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
