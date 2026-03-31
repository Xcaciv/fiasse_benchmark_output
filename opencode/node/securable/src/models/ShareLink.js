const { getDb } = require('./database');
const { v4: uuidv4 } = require('uuid');
const logger = require('../utils/logger');

class ShareLink {
  static async create(noteId, expiresAt = null) {
    const db = getDb();
    const token = uuidv4();
    
    const stmt = db.prepare(`
      INSERT INTO share_links (note_id, token, expires_at)
      VALUES (?, ?, ?)
    `);
    
    const result = stmt.run(noteId, token, expiresAt);
    
    logger.info('Share link created', { shareLinkId: result.lastInsertRowid, noteId });
    
    return { id: result.lastInsertRowid, token };
  }

  static async findById(id) {
    const db = getDb();
    const stmt = db.prepare('SELECT * FROM share_links WHERE id = ?');
    return stmt.get(id);
  }

  static async findByNoteId(noteId) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT * FROM share_links 
      WHERE note_id = ? AND is_active = 1
      AND (expires_at IS NULL OR expires_at > datetime('now'))
      ORDER BY created_at DESC
    `);
    return stmt.all(noteId);
  }

  static async findByToken(token) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT * FROM share_links 
      WHERE token = ? AND is_active = 1
      AND (expires_at IS NULL OR expires_at > datetime('now'))
    `);
    return stmt.get(token);
  }

  static async deactivate(id) {
    const db = getDb();
    const stmt = db.prepare('UPDATE share_links SET is_active = 0 WHERE id = ?');
    stmt.run(id);
    
    logger.info('Share link deactivated', { shareLinkId: id });
    return true;
  }

  static async deactivateByNoteId(noteId) {
    const db = getDb();
    const stmt = db.prepare('UPDATE share_links SET is_active = 0 WHERE note_id = ?');
    stmt.run(noteId);
    
    logger.info('Share links deactivated for note', { noteId });
    return true;
  }

  static async regenerate(noteId, expiresAt = null) {
    await this.deactivateByNoteId(noteId);
    return this.create(noteId, expiresAt);
  }

  static async delete(id) {
    const db = getDb();
    const stmt = db.prepare('DELETE FROM share_links WHERE id = ?');
    stmt.run(id);
    
    logger.info('Share link deleted', { shareLinkId: id });
    return true;
  }
}

module.exports = ShareLink;
