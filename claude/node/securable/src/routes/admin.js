'use strict';

const express = require('express');
const { body, validationResult } = require('express-validator');

const db     = require('../config/db');
const logger = require('../config/logger');
const { requireAdmin } = require('../middleware/auth');

const router = express.Router();

router.use(requireAdmin);

function logActivity(userId, action, details, ip) {
  try {
    db.prepare(`INSERT INTO activity_log (user_id, action, details, ip_address)
                VALUES (?, ?, ?, ?)`).run(userId || null, action, details || null, ip || null);
  } catch (_) { /* non-fatal */ }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /admin  — dashboard
// ─────────────────────────────────────────────────────────────────────────────
router.get('/', (req, res) => {
  const userCount = db.prepare('SELECT COUNT(*) AS cnt FROM users').get().cnt;
  const noteCount = db.prepare('SELECT COUNT(*) AS cnt FROM notes').get().cnt;
  const recentActivity = db.prepare(
    `SELECT al.*, u.username FROM activity_log al
     LEFT JOIN users u ON u.id = al.user_id
     ORDER BY al.created_at DESC LIMIT 50`
  ).all();

  res.render('admin/dashboard', {
    title: 'Admin Dashboard',
    userCount,
    noteCount,
    recentActivity
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /admin/users  — list all users
// ─────────────────────────────────────────────────────────────────────────────
router.get('/users', (req, res) => {
  const q = (req.query.q || '').trim();
  let users;

  if (q) {
    const pattern = `%${q}%`;
    users = db.prepare(
      `SELECT u.*, COUNT(n.id) AS note_count
       FROM users u LEFT JOIN notes n ON n.user_id = u.id
       WHERE u.username LIKE ? OR u.email LIKE ?
       GROUP BY u.id ORDER BY u.created_at DESC`
    ).all(pattern, pattern);
  } else {
    users = db.prepare(
      `SELECT u.*, COUNT(n.id) AS note_count
       FROM users u LEFT JOIN notes n ON n.user_id = u.id
       GROUP BY u.id ORDER BY u.created_at DESC`
    ).all();
  }

  res.render('admin/users', { title: 'Users', users, q });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /admin/notes/:id/reassign  — change note owner (REQ-016)
// ─────────────────────────────────────────────────────────────────────────────
router.post('/notes/:id/reassign', [
  body('new_user_id').isInt({ min: 1 }).withMessage('A valid user ID is required')
], (req, res) => {
  const noteId = parseInt(req.params.id, 10);
  const errors = validationResult(req);

  if (!errors.isEmpty()) {
    req.session.flash = { error: errors.array()[0].msg };
    return res.redirect('/admin/users');
  }

  const newUserId = parseInt(req.body.new_user_id, 10);
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(noteId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  const newOwner = db.prepare('SELECT id, username FROM users WHERE id = ?').get(newUserId);
  if (!newOwner) {
    req.session.flash = { error: 'Target user not found.' };
    return res.redirect('/admin/users');
  }

  const oldUserId = note.user_id;
  db.prepare('UPDATE notes SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?')
    .run(newUserId, noteId);

  logActivity(req.session.userId, 'note_reassigned',
    `noteId=${noteId} from userId=${oldUserId} to userId=${newUserId}`, req.ip);
  logger.info('Note ownership reassigned', {
    adminId: req.session.userId, noteId, oldUserId, newUserId
  });

  req.session.flash = { success: `Note reassigned to ${newOwner.username}.` };
  res.redirect('/admin/users');
});

module.exports = router;
