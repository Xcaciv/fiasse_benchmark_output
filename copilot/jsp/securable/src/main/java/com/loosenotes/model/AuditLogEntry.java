package com.loosenotes.model;

import java.time.LocalDateTime;

/** Immutable audit log entry (Accountability). No sensitive data stored. */
public final class AuditLogEntry {

    private final long id;
    private final Long actorId;
    private final String actorUsername;
    private final String action;
    private final String resourceType;
    private final String resourceId;
    private final String ipAddress;
    private final String outcome;
    private final String detail;
    private final LocalDateTime createdAt;

    public AuditLogEntry(long id, Long actorId, String actorUsername,
                         String action, String resourceType, String resourceId,
                         String ipAddress, String outcome, String detail,
                         LocalDateTime createdAt) {
        this.id             = id;
        this.actorId        = actorId;
        this.actorUsername  = actorUsername;
        this.action         = action;
        this.resourceType   = resourceType;
        this.resourceId     = resourceId;
        this.ipAddress      = ipAddress;
        this.outcome        = outcome;
        this.detail         = detail;
        this.createdAt      = createdAt;
    }

    public long getId()                  { return id; }
    public Long getActorId()             { return actorId; }
    public String getActorUsername()     { return actorUsername; }
    public String getAction()            { return action; }
    public String getResourceType()      { return resourceType; }
    public String getResourceId()        { return resourceId; }
    public String getIpAddress()         { return ipAddress; }
    public String getOutcome()           { return outcome; }
    public String getDetail()            { return detail; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
