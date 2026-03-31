package com.loosenotes.model;

import java.time.Instant;

/**
 * Represents a single entry in the security audit trail.
 *
 * SSEM notes:
 * - Accountability: records who did what, where, and when.
 * - Confidentiality: ipAddress is truncated (/24 for IPv4) by AuditService.
 *   No passwords or tokens stored here.
 */
public class AuditLog {

    /** Typed event categories to avoid free-text event classification. */
    public enum EventType {
        AUTH,
        NOTE,
        ADMIN,
        SHARE,
        ATTACHMENT,
        RATING
    }

    private long id;
    /** Nullable – unauthenticated actions (e.g., failed login) have no userId. */
    private Long userId;
    private EventType eventType;
    private String eventDetail;
    private String ipAddress;
    private Instant createdAt;

    public AuditLog() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getEventDetail() { return eventDetail; }
    public void setEventDetail(String eventDetail) { this.eventDetail = eventDetail; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "AuditLog{id=" + id + ", userId=" + userId
                + ", eventType=" + eventType + ", detail='" + eventDetail + "'}";
    }
}
