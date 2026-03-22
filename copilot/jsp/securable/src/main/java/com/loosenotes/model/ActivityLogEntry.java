package com.loosenotes.model;

public class ActivityLogEntry {
    private final long id;
    private final String actorUsername;
    private final String actionType;
    private final String details;
    private final String createdAt;

    public ActivityLogEntry(long id, String actorUsername, String actionType, String details, String createdAt) {
        this.id = id;
        this.actorUsername = actorUsername;
        this.actionType = actionType;
        this.details = details;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getActorUsername() { return actorUsername; }
    public String getActionType() { return actionType; }
    public String getDetails() { return details; }
    public String getCreatedAt() { return createdAt; }
}
