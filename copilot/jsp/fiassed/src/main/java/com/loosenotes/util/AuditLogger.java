package com.loosenotes.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class AuditLogger {
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AuditLogger INSTANCE = new AuditLogger();

    private AuditLogger() {}

    public static AuditLogger getInstance() {
        return INSTANCE;
    }

    public void log(String eventType, Long actorUserId, String targetEntityId,
                    String sourceIp, String outcome, String sessionId, String extraData) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("timestamp", Instant.now().toString());
            node.put("event_type", eventType);
            if (actorUserId != null) node.put("actor_user_id", actorUserId);
            if (targetEntityId != null) node.put("target_entity_id", targetEntityId);
            if (sourceIp != null) node.put("source_ip", sourceIp);
            if (outcome != null) node.put("outcome", outcome);
            if (sessionId != null) node.put("session_id_hash", SecurityUtils.sha256Hex(sessionId));
            if (extraData != null) node.put("extra_data", extraData);
            auditLog.info(MAPPER.writeValueAsString(node));
        } catch (Exception e) {
            LoggerFactory.getLogger(AuditLogger.class).error("Failed to write audit log", e);
        }
    }
}
