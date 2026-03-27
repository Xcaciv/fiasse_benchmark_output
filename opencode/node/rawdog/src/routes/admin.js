const express = require('express');
const { body, validationResult } = require('express-validator');
const { db } = require('../db/database');
const { isAuthenticated, isAdmin } = require('../middleware/auth');

const router = express.Router();

router.get('/', isAuthenticated, isAdmin, (req, res) => {
  const totalUsers = db.prepare('SELECT COUNT(*) as count FROM users').get().count;
  const totalNotes = db.prepare('SELECT COUNT(*) as count FROM notes').get().count;
  
  const recentActivity = db.prepare(`
    SELECT al.*, u.username 
    FROM activity_logs al
    LEFT JOIN users u ON al.user_id = u.id
    ORDER BY al.created_at DESC
    LIMIT 20
  `).all();

  res.render('admin/dashboard', { 
    user: req.session.user,
    stats: { totalUsers, totalNotes },
    recentActivity
  });
});

router.get('/users', isAuthenticated, isAdmin, (req, res) => {
  const { q } = req.query;
  
  let users;
  
  if (q && q.trim() !== '') {
    const searchTerm = `%${q}%`;
    users = db.prepare(`
      SELECT u.*, 
        (SELECT COUNT(*) FROM notes WHERE user_id = u.id) as note_count
      FROM users u
      WHERE u.username LIKE ? OR u.email LIKE ?
      ORDER BY u.created_at DESC
    `).all(searchTerm, searchTerm);
  } else {
    users = db.prepare(`
      SELECT u.*, 
        (SELECT COUNT(*) FROM notes WHERE user_id = u.id) as note_count
      FROM users u
      ORDER BY u.created_at DESC
    `).all();
  }

  res.render('admin/users', { 
    user: req.session.user,
    users,
    query: q || ''
  });
});

router.get('/notes', isAuthenticated, isAdmin, (req, res) => {
  const { q, userId } = req.query;
  
  let notes;
  
  if (userId) {
    notes = db.prepare(`
      SELECT n.*, u.username as author
      FROM notes n
      JOIN users u ON n.user_id = u.id
      WHERE n.user_id = ?
      ORDER BY n.updated_at DESC
    `).all(userId);
  } else if (q && q.trim() !== '') {
    const searchTerm = `%${q}%`;
    notes = db.prepare(`
      SELECT n.*, u.username as author
      FROM notes n
      JOIN users u ON n.user_id = u.id
      WHERE n.title LIKE ? OR n.content LIKE ?
      ORDER BY n.updated_at DESC
    `).all(searchTerm, searchTerm);
  } else {
    notes = db.prepare(`
      SELECT n.*, u.username as author
      FROM notes n
      JOIN users u ON n.user_id = u.id
      ORDER BY n.updated_at DESC
    `).all(100);
  }

  res.render('admin/notes', { 
    user: req.session.user,
    notes,
    query: q || '',
    selectedUserId: userId || null
  });
});

router.get('/notes/:id/reassign', isAuthenticated, isAdmin, (req, res) => {
  const note = db.prepare(`
    SELECT n.*, u.username as author
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.id = ?
  `).get(req.params.id);

  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: req.session.user });
  }

  const users = db.prepare('SELECT id, username FROM users ORDER BY username').all();

  res.render('admin/reassign', { 
    user: req.session.user,
    note,
    users,
    errors: [],
    formData: {}
  });
});

router.post('/notes/:id/reassign', isAuthenticated, isAdmin, [
  body('newOwnerId').isInt().withMessage('Invalid user selected')
], (req, res) => {
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);
  
  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: req.session.user });
  }

  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const users = db.prepare('SELECT id, username FROM users ORDER BY username').all();
    return res.render('admin/reassign', { 
      user: req.session.user,
      note,
      users,
      errors: errors.array(),
      formData: req.body
    });
  }

  const { newOwnerId } = req.body;
  const newOwner = db.prepare('SELECT id, username FROM users WHERE id = ?').get(newOwnerId);

  if (!newOwner) {
    const users = db.prepare('SELECT id, username FROM users ORDER BY username').all();
    return res.render('admin/reassign', { 
      user: req.session.user,
      note,
      users,
      errors: [{ msg: 'User not found' }],
      formData: req.body
    });
  }

  const oldOwner = db.prepare('SELECT username FROM users WHERE id = ?').get(note.user_id);

  db.prepare('UPDATE notes SET user_id = ?, updated_at = datetime(\'now\') WHERE id = ?').run(newOwnerId, note.id);

  db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
    req.session.user.id,
    'note_reassigned',
    `Reassigned note "${note.title}" from ${oldOwner.username} to ${newOwner.username}`
  );

  req.flash('success', `Note reassigned to ${newOwner.username}`);
  res.redirect('/admin/notes');
});

module.exports = router;
