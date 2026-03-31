const { getDb } = require('./database');
const logger = require('../utils/logger');

class Rating {
  static async create(noteId, userId, value, comment = null) {
    const db = getDb();
    
    const existingStmt = db.prepare('SELECT id FROM ratings WHERE note_id = ? AND user_id = ?');
    const existing = existingStmt.get(noteId, userId);
    
    if (existing) {
      return this.update(noteId, userId, value, comment);
    }
    
    const stmt = db.prepare(`
      INSERT INTO ratings (note_id, user_id, value, comment)
      VALUES (?, ?, ?, ?)
    `);
    
    const result = stmt.run(noteId, userId, value, comment);
    
    logger.info('Rating created', { ratingId: result.lastInsertRowid, noteId, userId });
    
    return result.lastInsertRowid;
  }

  static async update(noteId, userId, value, comment = null) {
    const db = getDb();
    const stmt = db.prepare(`
      UPDATE ratings SET value = ?, comment = ?, updated_at = CURRENT_TIMESTAMP
      WHERE note_id = ? AND user_id = ?
    `);
    
    stmt.run(value, comment, noteId, userId);
    
    logger.info('Rating updated', { noteId, userId });
    return true;
  }

  static async findById(id) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT r.*, u.username
      FROM ratings r
      JOIN users u ON r.user_id = u.id
      WHERE r.id = ?
    `);
    return stmt.get(id);
  }

  static async findByNoteId(noteId) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT r.*, u.username
      FROM ratings r
      JOIN users u ON r.user_id = u.id
      WHERE r.note_id = ?
      ORDER BY r.created_at DESC
    `);
    return stmt.all(noteId);
  }

  static async findByUserAndNote(userId, noteId) {
    const db = getDb();
    const stmt = db.prepare('SELECT * FROM ratings WHERE user_id = ? AND note_id = ?');
    return stmt.get(userId, noteId);
  }

  static async delete(id) {
    const db = getDb();
    const stmt = db.prepare('DELETE FROM ratings WHERE id = ?');
    stmt.run(id);
    
    logger.info('Rating deleted', { ratingId: id });
    return true;
  }

  static async getAverageRating(noteId) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT AVG(value) as avg_rating, COUNT(*) as count
      FROM ratings WHERE note_id = ?
    `);
    return stmt.get(noteId);
  }
}

module.exports = Rating;
