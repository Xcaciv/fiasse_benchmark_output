'use strict';

function createAttachment(db, { id, noteId, userId, originalFilename, storedFilename, mimeType, fileSize }) {
  const now = Date.now();
  db.prepare(
    'INSERT INTO attachments (id, note_id, user_id, original_filename, stored_filename, mime_type, file_size, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
  ).run(id, noteId, userId, originalFilename, storedFilename, mimeType, fileSize, now);
  return findById(db, id);
}

function findByNoteId(db, noteId) {
  return db.prepare('SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at ASC').all(noteId);
}

function findById(db, id) {
  return db.prepare('SELECT * FROM attachments WHERE id = ?').get(id) || null;
}

function deleteById(db, id) {
  const result = db.prepare('DELETE FROM attachments WHERE id = ?').run(id);
  return result.changes;
}

function deleteByNoteId(db, noteId) {
  const attachments = db.prepare('SELECT stored_filename FROM attachments WHERE note_id = ?').all(noteId);
  db.prepare('DELETE FROM attachments WHERE note_id = ?').run(noteId);
  return attachments.map(a => a.stored_filename);
}

module.exports = { createAttachment, findByNoteId, findById, deleteById, deleteByNoteId };
