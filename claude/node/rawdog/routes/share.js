const express = require('express');
const router = express.Router();
const { getDb } = require('../database/db');

// GET /share/:token - view shared note without authentication
router.get('/:token', (req, res) => {
  const db = getDb();
  const link = db.prepare('SELECT * FROM share_links WHERE token = ?').get(req.params.token);

  if (!link) {
    return res.status(404).render('error', {
      title: 'Link Not Found',
      message: 'This share link is invalid or has been revoked.',
      status: 404
    });
  }

  const note = db.prepare(`
    SELECT n.*, u.username
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.id = ?
  `).get(link.note_id);

  if (!note) {
    return res.status(404).render('error', {
      title: 'Not Found',
      message: 'The shared note no longer exists.',
      status: 404
    });
  }

  const attachments = db.prepare('SELECT * FROM attachments WHERE note_id = ?').all(note.id);
  const ratings = db.prepare(`
    SELECT r.*, u.username
    FROM ratings r
    JOIN users u ON r.user_id = u.id
    WHERE r.note_id = ?
    ORDER BY r.updated_at DESC
  `).all(note.id);

  const avgRating = ratings.length > 0
    ? (ratings.reduce((s, r) => s + r.rating, 0) / ratings.length).toFixed(1)
    : null;

  res.render('notes/share', {
    title: note.title,
    note,
    attachments,
    ratings,
    avgRating
  });
});

module.exports = router;
