package com.loosenotes.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loosenotes.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured JSON audit logger.
 *
 * <p>All security-relevant events — authentication, authorization, data access,
 * and administrative actions — must be recorded through this class. Each entry
 * is emitted as a single-line JSON object to the dedicated {@code AUDIT} logger,
 * enabling downstream ingestion by log management systems without additional
 * parsing.</p>
 *
 * <p><strong>Security contract</strong>: this class must <em>never</em> include
 * passwords, plaintext tokens, or password hashes in audit output. The
 * {@link AuditEvent} fields are designed to carry only identifiers and
 * structural metadata.</p>
 */
public final class AuditLogger {

    /**
     * Dedicated audit logger — configure this logger in logback.xml to route
     * to a separate, append-only audit log file.
     */
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    /**
     * Shared, thread-safe ObjectMapper configured for ISO-8601 timestamps.
     * Jackson's ObjectMapper is thread-safe after configuration is complete.
     */
    private static final ObjectMapper MAPPER = buildMapper();

    private AuditLogger() {
        // utility class — no instantiation
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Serialize an {@link AuditEvent} to JSON and emit it at INFO level on the
     * {@code AUDIT} logger.
     *
     * <p>If serialization fails for any reason the error is recorded on the
     * standard application logger and a minimal plain-text fallback is written
     * to the audit log so that the event is never silently dropped.</p>
     *
     * @param event the audit event to record; null events are silently ignored
     */
    public static void log(AuditEvent event) {
        if (event == null) {
            log.warn("AuditLogger.log called with null event — skipping");
            return;
        }

        try {
            Map<String, Object> entry = buildEntry(event);
            String json = MAPPER.writeValueAsString(entry);
            AUDIT.info(json);
        } catch (Exception e) {
            // Serialization must not suppress the audit record entirely.
            log.error("Failed to serialize audit event to JSON: {}", e.getMessage(), e);
            // Fallback: emit a safe, partial record.
            AUDIT.info("{\"event_type\":\"{}\",\"actor_id\":\"{}\",\"outcome\":\"{}\",\"error\":\"serialization_failure\"}",
                    safeString(event.getEventType()),
                    safeString(event.getActorId() != null ? event.getActorId().toString() : null),
                    safeString(event.getOutcome()));
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Build a structured map from an {@link AuditEvent}, explicitly enumerating
     * only the fields that are safe to emit. This acts as a final guard against
     * accidentally including sensitive fields that may be added to the model later.
     */
    private static Map<String, Object> buildEntry(AuditEvent event) {
        Map<String, Object> entry = new LinkedHashMap<>();

        // Temporal context
        entry.put("timestamp", event.getTimestamp() != null
                ? event.getTimestamp().toString()
                : java.time.Instant.now().toString());

        // Event classification
        entry.put("event_type",    safeString(event.getEventType()));

        // Actor context — identifiers only, never credentials
        entry.put("actor_id",       event.getActorId());
        entry.put("actor_username", safeString(event.getActorUsername()));
        entry.put("ip_address",     safeString(event.getIpAddress()));

        // Resource context
        entry.put("resource_type", safeString(event.getResourceType()));
        entry.put("resource_id",   event.getResourceId());

        // Outcome
        entry.put("outcome", safeString(event.getOutcome()));
        entry.put("detail",  safeString(event.getDetail()));

        return entry;
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /** Return the string value, or null if it is null (avoids NPE in map). */
    private static String safeString(String value) {
        return value;
    }
}
