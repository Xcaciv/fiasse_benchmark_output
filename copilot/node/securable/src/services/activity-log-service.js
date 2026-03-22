'use strict';

function logActivity(db, actorUserId, action, targetType, targetId, metadata = {}) {
  db.prepare(
    `INSERT INTO activity_logs (actor_user_id, action, target_type, target_id, metadata_json)
     VALUES (?, ?, ?, ?, ?)`
  ).run(actorUserId || null, action, targetType, targetId ? String(targetId) : null, JSON.stringify(metadata));
}

module.exports = { logActivity };
