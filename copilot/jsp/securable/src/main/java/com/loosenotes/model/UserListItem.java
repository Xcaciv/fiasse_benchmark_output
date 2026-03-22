package com.loosenotes.model;

public class UserListItem {
    private final long id;
    private final String username;
    private final String email;
    private final String role;
    private final String createdAt;
    private final int noteCount;

    public UserListItem(long id, String username, String email, String role, String createdAt, int noteCount) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.noteCount = noteCount;
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getCreatedAt() { return createdAt; }
    public int getNoteCount() { return noteCount; }
}
