'use strict';

// Append-only audit log — no updates or deletes allowed
function createAuditLog(db, { id, eventType, userId, ipAddress, resourceType, resourceId, details }) {
  db.prepare(
    'INSERT INTO audit_logs (id, event_type, user_id, ip_address, resource_type, resource_id, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
  ).run(
    id,
    eventType,
    userId || null,
    ipAddress || null,
    resourceType || null,
    resourceId || null,
    details ? JSON.stringify(details) : null,
    Date.now()
  );
}

module.exports = { createAuditLog };
