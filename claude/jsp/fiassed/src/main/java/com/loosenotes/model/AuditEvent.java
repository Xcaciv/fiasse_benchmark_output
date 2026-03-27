package com.loosenotes.model;

import java.time.LocalDateTime;

/**
 * Structured audit event model for GR-03 compliance.
 * Fields: timestamp, event_type, actor_id, ip_address, outcome, resource_id.
 * Sensitive values (tokens, passwords) are never stored here.
 */
public class AuditEvent {

    public enum Outcome {
        SUCCESS, FAILURE, DENIED
    }

    private Long id;
    private String eventType;
    private Long actorId;
    private String actorUsername;
    private String ipAddress;
    private String resourceType;
    private String resourceId;
    private Outcome outcome;
    private String detail;
    private LocalDateTime createdAt;

    // Builder pattern for readability at call sites
    public static Builder builder(String eventType, Outcome outcome) {
        return new Builder(eventType, outcome);
    }

    public static class Builder {
        private final AuditEvent event = new AuditEvent();

        public Builder(String eventType, Outcome outcome) {
            event.eventType = eventType;
            event.outcome = outcome;
            event.createdAt = LocalDateTime.now();
        }

        public Builder actor(Long actorId, String actorUsername) {
            event.actorId = actorId;
            event.actorUsername = actorUsername;
            return this;
        }

        public Builder ip(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }

        public Builder resource(String resourceType, String resourceId) {
            event.resourceType = resourceType;
            event.resourceId = resourceId;
            return this;
        }

        public Builder detail(String detail) {
            event.detail = detail;
            return this;
        }

        public AuditEvent build() {
            return event;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public Long getActorId() { return actorId; }
    public String getActorUsername() { return actorUsername; }
    public String getIpAddress() { return ipAddress; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public Outcome getOutcome() { return outcome; }
    public String getDetail() { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
