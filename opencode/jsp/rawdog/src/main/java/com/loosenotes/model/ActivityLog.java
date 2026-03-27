package com.loosenotes.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private Long id;
    private Long userId;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
    
    private User user;

    public ActivityLog() {
        this.createdAt = LocalDateTime.now();
    }

    public ActivityLog(Long userId, String action, String entityType, Long entityId, String details, String ipAddress) {
        this();
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static class Actions {
        public static final String LOGIN = "LOGIN";
        public static final String LOGOUT = "LOGOUT";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String REGISTER = "REGISTER";
        public static final String NOTE_CREATE = "NOTE_CREATE";
        public static final String NOTE_UPDATE = "NOTE_UPDATE";
        public static final String NOTE_DELETE = "NOTE_DELETE";
        public static final String NOTE_SHARE = "NOTE_SHARE";
        public static final String RATING_CREATE = "RATING_CREATE";
        public static final String RATING_UPDATE = "RATING_UPDATE";
        public static final String FILE_UPLOAD = "FILE_UPLOAD";
        public static final String FILE_DELETE = "FILE_DELETE";
        public static final String PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
        public static final String PASSWORD_RESET = "PASSWORD_RESET";
        public static final String ADMIN_NOTE_REASSIGN = "ADMIN_NOTE_REASSIGN";
        public static final String ADMIN_USER_MANAGEMENT = "ADMIN_USER_MANAGEMENT";
    }

    public static class Entities {
        public static final String USER = "USER";
        public static final String NOTE = "NOTE";
        public static final String RATING = "RATING";
        public static final String ATTACHMENT = "ATTACHMENT";
        public static final String SHARE_LINK = "SHARE_LINK";
    }
}
