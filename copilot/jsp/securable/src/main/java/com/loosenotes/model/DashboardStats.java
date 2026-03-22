package com.loosenotes.model;

public class DashboardStats {
    private final long totalUsers;
    private final long totalNotes;

    public DashboardStats(long totalUsers, long totalNotes) {
        this.totalUsers = totalUsers;
        this.totalNotes = totalNotes;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getTotalNotes() { return totalNotes; }
}
