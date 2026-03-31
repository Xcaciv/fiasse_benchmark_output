const { getDb } = require('./database');
const logger = require('../utils/logger');

class ActivityLog {
  static async log(userId, action, entityType = null, entityId = null, details = null, ipAddress = null) {
    const db = getDb();
    const stmt = db.prepare(`
      INSERT INTO activity_log (user_id, action, entity_type, entity_id, details, ip_address)
      VALUES (?, ?, ?, ?, ?, ?)
    `);
    
    const result = stmt.run(userId, action, entityType, entityId, details, ipAddress);
    
    return result.lastInsertRowid;
  }

  static async findRecent(limit = 50) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT al.*, u.username
      FROM activity_log al
      LEFT JOIN users u ON al.user_id = u.id
      ORDER BY al.created_at DESC
      LIMIT ?
    `);
    return stmt.all(limit);
  }

  static async findByUserId(userId, limit = 50) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT al.*, u.username
      FROM activity_log al
      LEFT JOIN users u ON al.user_id = u.id
      WHERE al.user_id = ?
      ORDER BY al.created_at DESC
      LIMIT ?
    `);
    return stmt.all(userId, limit);
  }

  static async count() {
    const db = getDb();
    const stmt = db.prepare('SELECT COUNT(*) as count FROM activity_log');
    return stmt.get().count;
  }

  static async getActionStats(days = 30) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT action, COUNT(*) as count
      FROM activity_log
      WHERE created_at >= datetime('now', '-' || ? || ' days')
      GROUP BY action
      ORDER BY count DESC
    `);
    return stmt.all(days);
  }
}

module.exports = ActivityLog;
