package com.loosenotes.dao;

import com.loosenotes.model.AuditLog;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access layer for AuditLog entries.
 * SSEM: Accountability - structured security event persistence.
 * SSEM: Resilience - log failures must not break application flow (logged only).
 */
public class AuditLogDao {

    private final DataSource dataSource;

    public AuditLogDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Records a new audit log entry. */
    public void create(AuditLog entry) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, resource_type, resource_id, "
            + "details, ip_address) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableLong(ps, 1, entry.getUserId());
            ps.setString(2, entry.getAction());
            ps.setString(3, entry.getResourceType());
            setNullableLong(ps, 4, entry.getResourceId());
            ps.setString(5, truncate(entry.getDetails(), 500));
            ps.setString(6, entry.getIpAddress());
            ps.executeUpdate();
        }
    }

    /** Returns recent audit log entries for admin dashboard. */
    public List<AuditLog> findRecent(int limit) throws SQLException {
        String sql = "SELECT al.*, u.username AS username FROM audit_logs al "
            + "LEFT JOIN users u ON u.id = al.user_id "
            + "ORDER BY al.created_at DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog entry = new AuditLog();
        entry.setId(rs.getLong("id"));
        long userId = rs.getLong("user_id");
        entry.setUserId(rs.wasNull() ? null : userId);
        entry.setAction(rs.getString("action"));
        entry.setResourceType(rs.getString("resource_type"));
        long resourceId = rs.getLong("resource_id");
        entry.setResourceId(rs.wasNull() ? null : resourceId);
        entry.setDetails(rs.getString("details"));
        entry.setIpAddress(rs.getString("ip_address"));
        entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return entry;
    }

    private void setNullableLong(PreparedStatement ps, int idx, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.BIGINT);
        } else {
            ps.setLong(idx, value);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
