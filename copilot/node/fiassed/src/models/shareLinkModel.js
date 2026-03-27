'use strict';

function createShareLink(db, { id, noteId, tokenHash }) {
  const now = Date.now();
  db.prepare(
    'INSERT INTO share_links (id, note_id, token_hash, created_at) VALUES (?, ?, ?, ?)'
  ).run(id, noteId, tokenHash, now);
  return findByNoteId(db, noteId);
}

function findByNoteId(db, noteId) {
  return db.prepare('SELECT * FROM share_links WHERE note_id = ?').get(noteId) || null;
}

function findByTokenHash(db, tokenHash) {
  return db.prepare(`
    SELECT sl.*, n.title, n.content, n.user_id, n.visibility
    FROM share_links sl
    JOIN notes n ON sl.note_id = n.id
    WHERE sl.token_hash = ?
  `).get(tokenHash) || null;
}

function deleteByNoteId(db, noteId) {
  const result = db.prepare('DELETE FROM share_links WHERE note_id = ?').run(noteId);
  return result.changes;
}

module.exports = { createShareLink, findByNoteId, findByTokenHash, deleteByNoteId };
