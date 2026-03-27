'use strict';

function createNote(db, { id, userId, title, content, visibility }) {
  const now = Date.now();
  db.prepare(
    'INSERT INTO notes (id, user_id, title, content, visibility, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)'
  ).run(id, userId, title, content, visibility || 'private', now, now);
  return findById(db, id);
}

function findById(db, id) {
  return db.prepare('SELECT * FROM notes WHERE id = ?').get(id) || null;
}

function findByIdForUser(db, id, userId) {
  return db.prepare(
    "SELECT * FROM notes WHERE id = ? AND (user_id = ? OR visibility = 'public')"
  ).get(id, userId) || null;
}

function findAllByUser(db, userId) {
  return db.prepare(
    'SELECT * FROM notes WHERE user_id = ? ORDER BY updated_at DESC'
  ).all(userId);
}

function updateNote(db, id, { title, content, visibility }) {
  const result = db.prepare(
    'UPDATE notes SET title = ?, content = ?, visibility = ?, updated_at = ? WHERE id = ?'
  ).run(title, content, visibility, Date.now(), id);
  return result.changes;
}

function deleteNote(db, id) {
  const result = db.prepare('DELETE FROM notes WHERE id = ?').run(id);
  return result.changes;
}

function getPublicNotesWithMinRatings(db, minRatings) {
  return db.prepare(`
    SELECT n.*, u.username,
           AVG(r.rating) AS avg_rating,
           COUNT(r.id) AS rating_count
    FROM notes n
    JOIN users u ON n.user_id = u.id
    LEFT JOIN ratings r ON r.note_id = n.id
    WHERE n.visibility = 'public'
    GROUP BY n.id
    HAVING rating_count >= ? AND avg_rating >= 1
    ORDER BY avg_rating DESC, rating_count DESC
  `).all(minRatings);
}

function searchNotes(db, userId, query) {
  const like = `%${query}%`;
  return db.prepare(
    "SELECT * FROM notes WHERE (user_id = ? OR visibility = 'public') AND (title LIKE ? OR content LIKE ?) ORDER BY updated_at DESC LIMIT 50"
  ).all(userId, like, like);
}

module.exports = { createNote, findById, findByIdForUser, findAllByUser, updateNote, deleteNote, getPublicNotesWithMinRatings, searchNotes };
