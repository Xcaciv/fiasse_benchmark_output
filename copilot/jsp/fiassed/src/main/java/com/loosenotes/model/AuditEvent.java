package com.loosenotes.model;

import java.time.LocalDateTime;

public class AuditEvent {
    private Long id;
    private LocalDateTime timestamp;
    private String eventType;
    private Long actorUserId;
    private String targetEntityId;
    private String sourceIp;
    private String outcome;
    private String sessionIdHash;
    private String extraData;

    public AuditEvent() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(String targetEntityId) { this.targetEntityId = targetEntityId; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getSessionIdHash() { return sessionIdHash; }
    public void setSessionIdHash(String sessionIdHash) { this.sessionIdHash = sessionIdHash; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
}
