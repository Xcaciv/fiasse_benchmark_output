package com.loosenotes.dao;

import com.loosenotes.model.AuditEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDao {

    public void insert(Connection conn, AuditEvent event) throws SQLException {
        String sql = "INSERT INTO audit_log (event_type, actor_user_id, target_entity_id, source_ip, outcome, session_id_hash, extra_data) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.getEventType());
            if (event.getActorUserId() != null) ps.setLong(2, event.getActorUserId());
            else ps.setNull(2, Types.BIGINT);
            ps.setString(3, event.getTargetEntityId());
            ps.setString(4, event.getSourceIp());
            ps.setString(5, event.getOutcome());
            ps.setString(6, event.getSessionIdHash());
            ps.setString(7, event.getExtraData());
            ps.executeUpdate();
        }
    }

    public List<AuditEvent> findRecent(Connection conn, int limit) throws SQLException {
        String sql = "SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            List<AuditEvent> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        }
    }

    private AuditEvent mapRow(ResultSet rs) throws SQLException {
        AuditEvent e = new AuditEvent();
        e.setId(rs.getLong("id"));
        e.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        e.setEventType(rs.getString("event_type"));
        long actorId = rs.getLong("actor_user_id");
        if (!rs.wasNull()) e.setActorUserId(actorId);
        e.setTargetEntityId(rs.getString("target_entity_id"));
        e.setSourceIp(rs.getString("source_ip"));
        e.setOutcome(rs.getString("outcome"));
        e.setSessionIdHash(rs.getString("session_id_hash"));
        e.setExtraData(rs.getString("extra_data"));
        return e;
    }
}
