'use strict';

const express = require('express');
const db = require('../config/db');

const router = express.Router();

// ─────────────────────────────────────────────────────────────────────────────
// GET /search?q=keyword
// ─────────────────────────────────────────────────────────────────────────────
router.get('/', (req, res) => {
  const q = (req.query.q || '').trim();

  if (!q) {
    return res.render('search', { title: 'Search', results: [], q: '' });
  }

  const pattern     = `%${q}%`;
  const currentUserId = req.session.userId || null;

  // Returns:
  //   • all notes owned by the current user (any visibility)
  //   • public notes from other users
  //   • private notes from other users are excluded
  const results = db.prepare(
    `SELECT n.id, n.title, n.content, n.is_public, n.created_at, n.user_id,
            u.username,
            COALESCE(ROUND(AVG(r.stars),1), 0) AS avg_rating,
            COUNT(DISTINCT r.id) AS rating_count
     FROM notes n
     JOIN users u ON u.id = n.user_id
     LEFT JOIN ratings r ON r.note_id = n.id
     WHERE (n.title LIKE ? OR n.content LIKE ?)
       AND (n.is_public = 1 OR n.user_id = ?)
     GROUP BY n.id
     ORDER BY n.updated_at DESC
     LIMIT 100`
  ).all(pattern, pattern, currentUserId);

  res.render('search', { title: 'Search Results', results, q });
});

module.exports = router;
