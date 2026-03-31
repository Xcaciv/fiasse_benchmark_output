const { getDb } = require('./database');
const logger = require('../utils/logger');
const path = require('path');
const fs = require('fs');

class Attachment {
  static async create(noteId, originalFilename, storedFilename, mimeType, size) {
    const db = getDb();
    const stmt = db.prepare(`
      INSERT INTO attachments (note_id, original_filename, stored_filename, mime_type, size)
      VALUES (?, ?, ?, ?, ?)
    `);
    
    const result = stmt.run(noteId, originalFilename, storedFilename, mimeType, size);
    
    logger.info('Attachment created', { attachmentId: result.lastInsertRowid, noteId });
    
    return result.lastInsertRowid;
  }

  static async findById(id) {
    const db = getDb();
    const stmt = db.prepare('SELECT * FROM attachments WHERE id = ?');
    return stmt.get(id);
  }

  static async findByNoteId(noteId) {
    const db = getDb();
    const stmt = db.prepare('SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at ASC');
    return stmt.all(noteId);
  }

  static async delete(id) {
    const db = getDb();
    const attachment = await this.findById(id);
    
    if (!attachment) return false;
    
    const uploadDir = process.env.UPLOAD_DIR || './uploads';
    const filePath = path.join(uploadDir, attachment.stored_filename);
    
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
    
    const stmt = db.prepare('DELETE FROM attachments WHERE id = ?');
    stmt.run(id);
    
    logger.info('Attachment deleted', { attachmentId: id });
    return true;
  }

  static async deleteByNoteId(noteId) {
    const attachments = await this.findByNoteId(noteId);
    
    for (const attachment of attachments) {
      await this.delete(attachment.id);
    }
  }

  static async count() {
    const db = getDb();
    const stmt = db.prepare('SELECT COUNT(*) as count FROM attachments');
    return stmt.get().count;
  }
}

module.exports = Attachment;
