const bcrypt = require('bcryptjs');
const { getDb } = require('./database');
const logger = require('../utils/logger');

class User {
  static async create(username, email, password, role = 'user') {
    const db = getDb();
    const passwordHash = await bcrypt.hash(password, 12);
    
    const stmt = db.prepare(`
      INSERT INTO users (username, email, password_hash, role)
      VALUES (?, ?, ?, ?)
    `);
    
    const result = stmt.run(username.toLowerCase(), email.toLowerCase(), passwordHash, role);
    
    logger.info('User created', { userId: result.lastInsertRowid, username });
    
    return result.lastInsertRowid;
  }

  static async findById(id) {
    const db = getDb();
    const stmt = db.prepare('SELECT * FROM users WHERE id = ?');
    return stmt.get(id);
  }

  static async findByUsername(username) {
    const db = getDb();
    const stmt = db.prepare('SELECT * FROM users WHERE username = ?');
    return stmt.get(username.toLowerCase());
  }

  static async findByEmail(email) {
    const db = getDb();
    const stmt = db.prepare('SELECT * FROM users WHERE email = ?');
    return stmt.get(email.toLowerCase());
  }

  static async verifyPassword(user, password) {
    return bcrypt.compare(password, user.password_hash);
  }

  static async getAll(search = null) {
    const db = getDb();
    if (search) {
      const searchTerm = `%${search.toLowerCase()}%`;
      const stmt = db.prepare(`
        SELECT u.*, COUNT(n.id) as note_count
        FROM users u
        LEFT JOIN notes n ON u.id = n.user_id
        WHERE LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ?
        GROUP BY u.id
        ORDER BY u.created_at DESC
      `);
      return stmt.all(searchTerm, searchTerm);
    }
    
    const stmt = db.prepare(`
      SELECT u.*, COUNT(n.id) as note_count
      FROM users u
      LEFT JOIN notes n ON u.id = n.user_id
      GROUP BY u.id
      ORDER BY u.created_at DESC
    `);
    return stmt.all();
  }

  static async count() {
    const db = getDb();
    const stmt = db.prepare('SELECT COUNT(*) as count FROM users');
    return stmt.get().count;
  }

  static async update(id, updates) {
    const db = getDb();
    const allowedFields = ['username', 'email'];
    const setClause = [];
    const values = [];
    
    for (const field of allowedFields) {
      if (updates[field] !== undefined) {
        setClause.push(`${field} = ?`);
        values.push(field === 'username' ? updates[field].toLowerCase() : updates[field].toLowerCase());
      }
    }
    
    if (setClause.length === 0) return false;
    
    setClause.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);
    
    const stmt = db.prepare(`UPDATE users SET ${setClause.join(', ')} WHERE id = ?`);
    stmt.run(...values);
    
    logger.info('User updated', { userId: id });
    return true;
  }

  static async updatePassword(id, newPassword) {
    const db = getDb();
    const passwordHash = await bcrypt.hash(newPassword, 12);
    
    const stmt = db.prepare('UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?');
    stmt.run(passwordHash, id);
    
    logger.info('User password updated', { userId: id });
  }

  static async setPasswordResetToken(email, token, expiresAt) {
    const db = getDb();
    const stmt = db.prepare(`
      UPDATE users SET password_reset_token = ?, password_reset_expires = ?
      WHERE email = ?
    `);
    stmt.run(token, expiresAt, email.toLowerCase());
  }

  static async findByPasswordResetToken(token) {
    const db = getDb();
    const stmt = db.prepare(`
      SELECT * FROM users 
      WHERE password_reset_token = ? 
      AND password_reset_expires > datetime('now')
    `);
    return stmt.get(token);
  }

  static async clearPasswordResetToken(id) {
    const db = getDb();
    const stmt = db.prepare(`
      UPDATE users SET password_reset_token = NULL, password_reset_expires = NULL 
      WHERE id = ?
    `);
    stmt.run(id);
  }
}

module.exports = User;
