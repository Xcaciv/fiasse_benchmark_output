const { getDb } = require('./database');
const logger = require('../utils/logger');

class Note {
  static async create(userId, title, content, isPublic = false) {
    const db = getDb();
    const stmt = db.prepare(`
      INSERT INTO notes (user_id, title, content, is_public)
      VALUES (?, ?, ?, ?)
    `);
    
    const result = stmt.run(userId, title, content, isPublic ? 1 : 0);
    
    logger.info('Note created', { noteId: result.lastInsertRowid, userId });
    
    return result.lastInsertRowid;
  }

  static async findById(id, userId = null) {
    const db = getDb();
    let query = `
      SELECT n.*, u.username as author_username,
        (SELECT AVG(value) FROM ratings WHERE note_id = n.id) as avg_rating,
        (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
      FROM notes n
      JOIN users u ON n.user_id = u.id
      WHERE n.id = ?
    `;
    
    const params = [id];
    
    if (userId !== null) {
      query += ` AND (n.is_public = 1 OR n.user_id = ?)`;
      params.push(userId);
    }
    
    const stmt = db.prepare(query);
    return stmt.get(...params);
  }

  static async findByIdForShare(id, token) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT n.*, u.username as author_username,
        (SELECT AVG(value) FROM ratings WHERE note_id = n.id) as avg_rating,
        (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
      FROM notes n
      JOIN users u ON n.user_id = u.id
      JOIN share_links sl ON n.id = sl.note_id
      WHERE n.id = ? AND sl.token = ? AND sl.is_active = 1
      AND (sl.expires_at IS NULL OR sl.expires_at > datetime('now'))
    `);
    return stmt.get(id, token);
  }

  static async findByIdWithOwner(id) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT n.*, u.username as author_username
      FROM notes n
      JOIN users u ON n.user_id = u.id
      WHERE n.id = ?
    `);
    return stmt.get(id);
  }

  static async getByUserId(userId, search = null) {
    const db = getDb();
    let query = `
      SELECT n.*, 
        (SELECT AVG(value) FROM ratings WHERE note_id = n.id) as avg_rating,
        (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
      FROM notes n
      WHERE n.user_id = ?
    `;
    const params = [userId];
    
    if (search) {
      query += ` AND (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?)`;
      const searchTerm = `%${search.toLowerCase()}%`;
      params.push(searchTerm, searchTerm);
    }
    
    query += ` ORDER BY n.created_at DESC`;
    
    const stmt = db.prepare(query);
    return stmt.all(...params);
  }

  static async getPublicNotes(search = null) {
    const db = getDb();
    let query = `
      SELECT n.*, u.username as author_username,
        (SELECT AVG(value) FROM ratings WHERE note_id = n.id) as avg_rating,
        (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
      FROM notes n
      JOIN users u ON n.user_id = u.id
      WHERE n.is_public = 1
    `;
    const params = [];
    
    if (search) {
      query += ` AND (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?)`;
      const searchTerm = `%${search.toLowerCase()}%`;
      params.push(searchTerm, searchTerm);
    }
    
    query += ` ORDER BY n.created_at DESC`;
    
    const stmt = db.prepare(query);
    return stmt.all(...params);
  }

  static async search(userId, searchTerm) {
    const db = getDb();
    const search = `%${searchTerm.toLowerCase()}%`;
    
    const stmt = db.prepare(`
      SELECT n.*, u.username as author_username,
        (SELECT AVG(value) FROM ratings WHERE note_id = n.id) as avg_rating,
        (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
      FROM notes n
      JOIN users u ON n.user_id = u.id
      WHERE (n.is_public = 1 OR n.user_id = ?)
      AND (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?)
      ORDER BY n.created_at DESC
    `);
    
    return stmt.all(userId, search, search);
  }

  static async getTopRated(minRatings = 3) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT n.*, u.username as author_username,
        AVG(r.value) as avg_rating,
        COUNT(r.id) as rating_count
      FROM notes n
      JOIN users u ON n.user_id = u.id
      JOIN ratings r ON n.id = r.note_id
      WHERE n.is_public = 1
      GROUP BY n.id
      HAVING COUNT(r.id) >= ?
      ORDER BY avg_rating DESC, rating_count DESC
      LIMIT 50
    `);
    return stmt.all(minRatings);
  }

  static async update(id, userId, updates) {
    const db = getDb();
    const note = await this.findByIdWithOwner(id);
    
    if (!note || note.user_id !== userId) {
      return false;
    }
    
    const allowedFields = ['title', 'content', 'is_public'];
    const setClause = [];
    const values = [];
    
    for (const field of allowedFields) {
      if (updates[field] !== undefined) {
        setClause.push(`${field} = ?`);
        if (field === 'is_public') {
          values.push(updates[field] ? 1 : 0);
        } else {
          values.push(updates[field]);
        }
      }
    }
    
    if (setClause.length === 0) return false;
    
    setClause.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);
    
    const stmt = db.prepare(`UPDATE notes SET ${setClause.join(', ')} WHERE id = ?`);
    stmt.run(...values);
    
    logger.info('Note updated', { noteId: id, userId });
    return true;
  }

  static async delete(id, userId, isAdmin = false) {
    const db = getDb();
    let stmt;
    
    if (isAdmin) {
      stmt = db.prepare('DELETE FROM notes WHERE id = ?');
      stmt.run(id);
    } else {
      stmt = db.prepare('DELETE FROM notes WHERE id = ? AND user_id = ?');
      stmt.run(id, userId);
    }
    
    logger.info('Note deleted', { noteId: id, userId, isAdmin });
    return true;
  }

  static async reassignOwner(noteId, newUserId) {
    const db = getDb();
    const stmt = db.prepare('UPDATE notes SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?');
    stmt.run(newUserId, noteId);
    
    logger.info('Note owner reassigned', { noteId, newUserId });
    return true;
  }

  static async count() {
    const db = getDb();
    const stmt = db.prepare('SELECT COUNT(*) as count FROM notes');
    return stmt.get().count;
  }

  static async getRecent(limit = 10) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT n.*, u.username as author_username
      FROM notes n
      JOIN users u ON n.user_id = u.id
      ORDER BY n.created_at DESC
      LIMIT ?
    `);
    return stmt.all(limit);
  }
}

module.exports = Note;
