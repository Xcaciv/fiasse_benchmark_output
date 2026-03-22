package com.loosenotes.dao.impl;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.model.AuditLogEntry;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuditLogDaoImpl implements AuditLogDao {

    private static final Logger log = LoggerFactory.getLogger(AuditLogDaoImpl.class);
    private final DatabaseManager db;

    public AuditLogDaoImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void insert(AuditLogEntry entry) {
        final String sql =
            "INSERT INTO audit_log (actor_id, actor_username, action, resource_type, " +
            "resource_id, ip_address, outcome, detail) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (entry.getActorId() != null) ps.setLong(1, entry.getActorId());
            else                            ps.setNull(1, Types.INTEGER);
            ps.setString(2, entry.getActorUsername());
            ps.setString(3, entry.getAction());
            ps.setString(4, entry.getResourceType());
            ps.setString(5, entry.getResourceId());
            ps.setString(6, entry.getIpAddress());
            ps.setString(7, entry.getOutcome());
            ps.setString(8, entry.getDetail());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Audit persistence failure should not crash the application (Resilience)
            log.error("Failed to insert audit log entry action={}", entry.getAction(), e);
        }
    }

    @Override
    public List<AuditLogEntry> findRecent(int limit) {
        List<AuditLogEntry> results = new ArrayList<>();
        final String sql =
            "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findRecent audit_log failed", e);
        }
        return results;
    }

    private AuditLogEntry mapRow(ResultSet rs) throws SQLException {
        long actorIdVal = rs.getLong("actor_id");
        Long actorId = rs.wasNull() ? null : actorIdVal;
        String createdStr = rs.getString("created_at");
        LocalDateTime created = createdStr != null
                ? LocalDateTime.parse(createdStr.replace(" ", "T"))
                : LocalDateTime.now();
        return new AuditLogEntry(
            rs.getLong("id"),
            actorId,
            rs.getString("actor_username"),
            rs.getString("action"),
            rs.getString("resource_type"),
            rs.getString("resource_id"),
            rs.getString("ip_address"),
            rs.getString("outcome"),
            rs.getString("detail"),
            created
        );
    }
}
