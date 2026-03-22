const express = require('express');
const router = express.Router();
const { getDb, logActivity } = require('../database/db');
const { ensureAuthenticated, ensureAdmin } = require('../middleware/auth');

// GET /admin/dashboard
router.get('/dashboard', ensureAuthenticated, ensureAdmin, (req, res) => {
  const db = getDb();
  const userCount = db.prepare('SELECT COUNT(*) as count FROM users').get().count;
  const noteCount = db.prepare('SELECT COUNT(*) as count FROM notes').get().count;
  const recentActivity = db.prepare(`
    SELECT a.*, u.username
    FROM activity_log a
    LEFT JOIN users u ON a.user_id = u.id
    ORDER BY a.created_at DESC
    LIMIT 50
  `).all();

  res.render('admin/dashboard', {
    title: 'Admin Dashboard',
    userCount,
    noteCount,
    recentActivity
  });
});

// GET /admin/users
router.get('/users', ensureAuthenticated, ensureAdmin, (req, res) => {
  const db = getDb();
  const search = (req.query.search || '').trim();
  let users;

  if (search) {
    const term = `%${search}%`;
    users = db.prepare(`
      SELECT u.*, (SELECT COUNT(*) FROM notes WHERE user_id = u.id) as note_count
      FROM users u
      WHERE u.username LIKE ? OR u.email LIKE ?
      ORDER BY u.created_at DESC
    `).all(term, term);
  } else {
    users = db.prepare(`
      SELECT u.*, (SELECT COUNT(*) FROM notes WHERE user_id = u.id) as note_count
      FROM users u
      ORDER BY u.created_at DESC
    `).all();
  }

  res.render('admin/users', { title: 'Manage Users', users, search });
});

// GET /admin/users/:id/reassign
router.get('/users/:id/reassign', ensureAuthenticated, ensureAdmin, (req, res) => {
  const db = getDb();
  const targetUser = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);

  if (!targetUser) {
    return res.status(404).render('error', {
      title: 'Not Found', message: 'User not found.', status: 404
    });
  }

  const notes = db.prepare('SELECT * FROM notes WHERE user_id = ? ORDER BY created_at DESC').all(targetUser.id);
  const allUsers = db.prepare('SELECT id, username FROM users WHERE id != ? ORDER BY username').all(targetUser.id);

  res.render('admin/reassign', { title: 'Reassign Notes', targetUser, notes, allUsers });
});

// POST /admin/users/:id/reassign
router.post('/users/:id/reassign', ensureAuthenticated, ensureAdmin, (req, res) => {
  const db = getDb();
  const { note_id, new_user_id } = req.body;

  const targetUser = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
  const newUser = db.prepare('SELECT * FROM users WHERE id = ?').get(new_user_id);

  if (!targetUser || !newUser) {
    req.flash('error', 'Invalid user selection.');
    return res.redirect(`/admin/users/${req.params.id}/reassign`);
  }

  const note = db.prepare('SELECT * FROM notes WHERE id = ? AND user_id = ?').get(note_id, req.params.id);
  if (!note) {
    req.flash('error', 'Note not found or does not belong to this user.');
    return res.redirect(`/admin/users/${req.params.id}/reassign`);
  }

  db.prepare('UPDATE notes SET user_id = ? WHERE id = ?').run(new_user_id, note_id);

  logActivity(
    req.user.id,
    'note_reassigned',
    `Admin reassigned note "${note.title}" (id: ${note_id}) from ${targetUser.username} to ${newUser.username}`
  );

  req.flash('success', `Note reassigned to ${newUser.username} successfully.`);
  res.redirect(`/admin/users/${req.params.id}/reassign`);
});

module.exports = router;
