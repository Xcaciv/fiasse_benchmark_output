package com.loosenotes.dao;

import com.loosenotes.model.AuditLog;
import com.loosenotes.model.AuditLog.EventType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the audit_logs table.
 *
 * SSEM notes:
 * - Accountability: append-only inserts; no update/delete methods exposed.
 * - Confidentiality: ipAddress is pre-truncated by AuditService before this call.
 */
public class AuditLogDao {

    private final DatabaseManager db;

    public AuditLogDao(DatabaseManager db) {
        this.db = db;
    }

    /** Inserts a new audit log entry. */
    public void insert(AuditLog entry) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, event_type, event_detail, ip_address) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (entry.getUserId() != null) {
                ps.setLong(1, entry.getUserId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, entry.getEventType().name());
            ps.setString(3, entry.getEventDetail());
            ps.setString(4, entry.getIpAddress());
            ps.executeUpdate();
        }
    }

    /** Returns the most recent {@code limit} audit log entries (admin dashboard). */
    public List<AuditLog> findRecent(int limit) throws SQLException {
        String sql = "SELECT id, user_id, event_type, event_detail, ip_address, created_at "
                + "FROM audit_logs ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditLog> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        a.setUserId(rs.wasNull() ? null : uid);
        a.setEventType(EventType.valueOf(rs.getString("event_type")));
        a.setEventDetail(rs.getString("event_detail"));
        a.setIpAddress(rs.getString("ip_address"));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return a;
    }
}
