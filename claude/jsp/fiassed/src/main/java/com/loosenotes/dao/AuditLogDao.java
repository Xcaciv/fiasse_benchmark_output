package com.loosenotes.dao;

import com.loosenotes.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only data-access object for the {@code audit_logs} table.
 * This DAO exposes no UPDATE or DELETE operations by design (GR-03, Accountability).
 * Sensitive values (tokens, passwords) are never written to audit records.
 */
public class AuditLogDao {

    private static final Logger log = LoggerFactory.getLogger(AuditLogDao.class);

    private final DatabaseManager db = DatabaseManager.getInstance();

    /**
     * Appends a new audit event. This is the only write method — no UPDATE or DELETE.
     *
     * @param event fully-constructed AuditEvent (use AuditEvent.builder(...).build())
     * @return true if the row was inserted successfully
     */
    public boolean append(AuditEvent event) {
        final String sql =
            "INSERT INTO audit_logs " +
            "(event_type, actor_id, actor_username, ip_address, " +
            " resource_type, resource_id, outcome, detail) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, event.getEventType());
            ps.setObject(2, event.getActorId());        // nullable Long
            ps.setString(3, event.getActorUsername());  // nullable
            ps.setString(4, event.getIpAddress());      // nullable
            ps.setString(5, event.getResourceType());   // nullable
            ps.setString(6, event.getResourceId());     // nullable
            ps.setString(7, event.getOutcome().name());
            ps.setString(8, event.getDetail());         // nullable
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) event.setId(keys.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            // Log at WARN so audit failures are visible but don't break the calling thread.
            log.warn("audit append failed event_type={} outcome={}: {}",
                     event.getEventType(), event.getOutcome(), e.getMessage(), e);
        }
        return false;
    }

    /**
     * Returns the most recent audit entries for the admin dashboard.
     * Only non-sensitive fields are exposed; IP addresses are included for admin review.
     *
     * @param limit maximum number of rows to return
     */
    public List<AuditEvent> findRecent(int limit) {
        final String sql =
            "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ?";
        List<AuditEvent> events = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) events.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findRecent failed limit={}: {}", limit, e.getMessage(), e);
        }
        return events;
    }

    // --------------------------------------------------------------- helpers

    private AuditEvent mapRow(ResultSet rs) throws SQLException {
        AuditEvent.Outcome outcome =
            AuditEvent.Outcome.valueOf(rs.getString("outcome"));

        AuditEvent.Builder builder =
            AuditEvent.builder(rs.getString("event_type"), outcome)
                .ip(rs.getString("ip_address"))
                .resource(rs.getString("resource_type"), rs.getString("resource_id"))
                .detail(rs.getString("detail"));

        long actorId = rs.getLong("actor_id");
        if (!rs.wasNull()) {
            builder.actor(actorId, rs.getString("actor_username"));
        }

        AuditEvent event = builder.build();
        event.setId(rs.getLong("id"));
        event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return event;
    }
}
