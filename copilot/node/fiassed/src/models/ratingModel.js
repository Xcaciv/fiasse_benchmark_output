'use strict';

function createRating(db, { id, noteId, userId, rating, comment }) {
  const now = Date.now();
  db.prepare(
    'INSERT INTO ratings (id, note_id, user_id, rating, comment, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)'
  ).run(id, noteId, userId, rating, comment || null, now, now);
  return findByNoteAndUser(db, noteId, userId);
}

function findByNoteId(db, noteId) {
  return db.prepare(`
    SELECT r.*, u.username
    FROM ratings r
    JOIN users u ON r.user_id = u.id
    WHERE r.note_id = ?
    ORDER BY r.created_at DESC
  `).all(noteId);
}

function findByNoteAndUser(db, noteId, userId) {
  return db.prepare('SELECT * FROM ratings WHERE note_id = ? AND user_id = ?').get(noteId, userId) || null;
}

function updateRating(db, id, { rating, comment }) {
  const result = db.prepare(
    'UPDATE ratings SET rating = ?, comment = ?, updated_at = ? WHERE id = ?'
  ).run(rating, comment || null, Date.now(), id);
  return result.changes;
}

function getAverageRating(db, noteId) {
  const row = db.prepare(
    'SELECT AVG(rating) AS avg, COUNT(*) AS count FROM ratings WHERE note_id = ?'
  ).get(noteId);
  return { avg: row.avg || 0, count: row.count || 0 };
}

module.exports = { createRating, findByNoteId, findByNoteAndUser, updateRating, getAverageRating };
