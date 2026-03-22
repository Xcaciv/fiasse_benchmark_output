const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const { v4: uuidv4 } = require('uuid');
const { getDb, logActivity } = require('../database/db');
const { ensureAuthenticated } = require('../middleware/auth');
const upload = require('../middleware/upload');

// GET /notes - list user's notes
router.get('/', ensureAuthenticated, (req, res) => {
  const db = getDb();
  const notes = db.prepare(`
    SELECT n.*,
           (SELECT COUNT(*) FROM attachments WHERE note_id = n.id) as attachment_count,
           ROUND((SELECT AVG(rating) FROM ratings WHERE note_id = n.id), 1) as avg_rating,
           (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
    FROM notes n
    WHERE n.user_id = ?
    ORDER BY n.updated_at DESC
  `).all(req.user.id);

  res.render('notes/index', { title: 'My Notes', notes });
});

// GET /notes/search
router.get('/search', (req, res) => {
  const db = getDb();
  const q = (req.query.q || '').trim();

  if (!q) {
    return res.render('notes/search', { title: 'Search Notes', notes: [], query: '' });
  }

  const term = `%${q}%`;
  let notes;

  if (req.isAuthenticated()) {
    notes = db.prepare(`
      SELECT n.*, u.username,
             ROUND((SELECT AVG(rating) FROM ratings WHERE note_id = n.id), 1) as avg_rating,
             (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
      FROM notes n
      JOIN users u ON n.user_id = u.id
      WHERE (n.title LIKE ? OR n.content LIKE ?)
        AND (n.user_id = ? OR n.visibility = 'public')
      ORDER BY n.updated_at DESC
    `).all(term, term, req.user.id);
  } else {
    notes = db.prepare(`
      SELECT n.*, u.username,
             ROUND((SELECT AVG(rating) FROM ratings WHERE note_id = n.id), 1) as avg_rating,
             (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
      FROM notes n
      JOIN users u ON n.user_id = u.id
      WHERE (n.title LIKE ? OR n.content LIKE ?)
        AND n.visibility = 'public'
      ORDER BY n.updated_at DESC
    `).all(term, term);
  }

  res.render('notes/search', { title: 'Search Results', notes, query: q });
});

// GET /notes/top-rated
router.get('/top-rated', (req, res) => {
  const db = getDb();
  const notes = db.prepare(`
    SELECT n.*, u.username,
           ROUND(AVG(r.rating), 1) as avg_rating,
           COUNT(r.id) as rating_count
    FROM notes n
    JOIN users u ON n.user_id = u.id
    JOIN ratings r ON r.note_id = n.id
    WHERE n.visibility = 'public'
    GROUP BY n.id
    HAVING COUNT(r.id) >= 3
    ORDER BY avg_rating DESC, rating_count DESC
    LIMIT 50
  `).all();

  res.render('notes/top-rated', { title: 'Top Rated Notes', notes });
});

// GET /notes/create
router.get('/create', ensureAuthenticated, (req, res) => {
  res.render('notes/create', { title: 'Create Note' });
});

// POST /notes/create
router.post('/create', ensureAuthenticated, (req, res, next) => {
  upload.array('attachments', 10)(req, res, (err) => {
    if (err) {
      req.flash('error', err.message);
      return res.redirect('/notes/create');
    }

    const db = getDb();
    const { title, content, visibility } = req.body;

    if (!title || !content) {
      req.flash('error', 'Title and content are required.');
      return res.redirect('/notes/create');
    }

    const vis = visibility === 'public' ? 'public' : 'private';
    const result = db.prepare(
      'INSERT INTO notes (user_id, title, content, visibility) VALUES (?, ?, ?, ?)'
    ).run(req.user.id, title, content, vis);

    const noteId = result.lastInsertRowid;

    if (req.files && req.files.length > 0) {
      const insertAtt = db.prepare(
        'INSERT INTO attachments (note_id, original_filename, stored_filename, file_size, mime_type) VALUES (?, ?, ?, ?, ?)'
      );
      for (const file of req.files) {
        insertAtt.run(noteId, file.originalname, file.filename, file.size, file.mimetype);
      }
    }

    logActivity(req.user.id, 'note_created', `Created note: ${title}`);
    req.flash('success', 'Note created successfully.');
    res.redirect(`/notes/${noteId}`);
  });
});

// GET /notes/:id - view note
router.get('/:id', (req, res) => {
  const db = getDb();
  const note = db.prepare(`
    SELECT n.*, u.username
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.id = ?
  `).get(req.params.id);

  if (!note) {
    return res.status(404).render('error', {
      title: 'Not Found', message: 'Note not found.', status: 404
    });
  }

  if (note.visibility === 'private') {
    if (!req.isAuthenticated() || (req.user.id !== note.user_id && req.user.role !== 'admin')) {
      return res.status(403).render('error', {
        title: 'Forbidden', message: 'This note is private.', status: 403
      });
    }
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

  let userRating = null;
  if (req.isAuthenticated()) {
    userRating = db.prepare(
      'SELECT * FROM ratings WHERE note_id = ? AND user_id = ?'
    ).get(note.id, req.user.id);
  }

  const shareLink = db.prepare('SELECT token FROM share_links WHERE note_id = ?').get(note.id);
  const isOwner = req.isAuthenticated() && req.user.id === note.user_id;
  const isAdmin = req.isAuthenticated() && req.user.role === 'admin';

  res.render('notes/view', {
    title: note.title,
    note, attachments, ratings, avgRating, userRating, shareLink, isOwner, isAdmin
  });
});

// GET /notes/:id/edit
router.get('/:id/edit', ensureAuthenticated, (req, res) => {
  const db = getDb();
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);

  if (!note) {
    return res.status(404).render('error', {
      title: 'Not Found', message: 'Note not found.', status: 404
    });
  }

  if (note.user_id !== req.user.id && req.user.role !== 'admin') {
    return res.status(403).render('error', {
      title: 'Forbidden', message: 'You cannot edit this note.', status: 403
    });
  }

  const attachments = db.prepare('SELECT * FROM attachments WHERE note_id = ?').all(note.id);
  res.render('notes/edit', { title: 'Edit Note', note, attachments });
});

// POST /notes/:id/edit
router.post('/:id/edit', ensureAuthenticated, (req, res) => {
  upload.array('attachments', 10)(req, res, (err) => {
    if (err) {
      req.flash('error', err.message);
      return res.redirect(`/notes/${req.params.id}/edit`);
    }

    const db = getDb();
    const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);

    if (!note) {
      return res.status(404).render('error', {
        title: 'Not Found', message: 'Note not found.', status: 404
      });
    }

    if (note.user_id !== req.user.id && req.user.role !== 'admin') {
      return res.status(403).render('error', {
        title: 'Forbidden', message: 'You cannot edit this note.', status: 403
      });
    }

    const { title, content, visibility } = req.body;

    if (!title || !content) {
      req.flash('error', 'Title and content are required.');
      return res.redirect(`/notes/${req.params.id}/edit`);
    }

    const vis = visibility === 'public' ? 'public' : 'private';
    db.prepare(`
      UPDATE notes SET title = ?, content = ?, visibility = ?, updated_at = datetime('now')
      WHERE id = ?
    `).run(title, content, vis, note.id);

    if (req.files && req.files.length > 0) {
      const insertAtt = db.prepare(
        'INSERT INTO attachments (note_id, original_filename, stored_filename, file_size, mime_type) VALUES (?, ?, ?, ?, ?)'
      );
      for (const file of req.files) {
        insertAtt.run(note.id, file.originalname, file.filename, file.size, file.mimetype);
      }
    }

    logActivity(req.user.id, 'note_updated', `Updated note: ${title}`);
    req.flash('success', 'Note updated successfully.');
    res.redirect(`/notes/${note.id}`);
  });
});

// POST /notes/:id/delete
router.post('/:id/delete', ensureAuthenticated, (req, res) => {
  const db = getDb();
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);

  if (!note) {
    return res.status(404).render('error', {
      title: 'Not Found', message: 'Note not found.', status: 404
    });
  }

  if (note.user_id !== req.user.id && req.user.role !== 'admin') {
    return res.status(403).render('error', {
      title: 'Forbidden', message: 'You cannot delete this note.', status: 403
    });
  }

  const attachments = db.prepare('SELECT * FROM attachments WHERE note_id = ?').all(note.id);
  for (const att of attachments) {
    const filePath = path.join(__dirname, '..', 'uploads', att.stored_filename);
    if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
  }

  db.prepare('DELETE FROM notes WHERE id = ?').run(note.id);
  logActivity(req.user.id, 'note_deleted', `Deleted note: ${note.title}`);
  req.flash('success', 'Note deleted successfully.');
  res.redirect('/notes');
});

// POST /notes/:id/share/generate
router.post('/:id/share/generate', ensureAuthenticated, (req, res) => {
  const db = getDb();
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);

  if (!note || note.user_id !== req.user.id) {
    return res.status(403).render('error', {
      title: 'Forbidden', message: 'Unauthorized.', status: 403
    });
  }

  db.prepare('DELETE FROM share_links WHERE note_id = ?').run(note.id);
  const token = uuidv4();
  db.prepare('INSERT INTO share_links (note_id, token) VALUES (?, ?)').run(note.id, token);

  req.flash('success', 'Share link generated.');
  res.redirect(`/notes/${note.id}`);
});

// POST /notes/:id/share/revoke
router.post('/:id/share/revoke', ensureAuthenticated, (req, res) => {
  const db = getDb();
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);

  if (!note || note.user_id !== req.user.id) {
    return res.status(403).render('error', {
      title: 'Forbidden', message: 'Unauthorized.', status: 403
    });
  }

  db.prepare('DELETE FROM share_links WHERE note_id = ?').run(note.id);
  req.flash('success', 'Share link revoked.');
  res.redirect(`/notes/${note.id}`);
});

// POST /notes/:id/rate
router.post('/:id/rate', ensureAuthenticated, (req, res) => {
  const db = getDb();
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);

  if (!note) {
    return res.status(404).render('error', {
      title: 'Not Found', message: 'Note not found.', status: 404
    });
  }

  const ratingVal = parseInt(req.body.rating, 10);
  if (isNaN(ratingVal) || ratingVal < 1 || ratingVal > 5) {
    req.flash('error', 'Rating must be between 1 and 5.');
    return res.redirect(`/notes/${note.id}`);
  }

  const comment = (req.body.comment || '').trim() || null;

  db.prepare(`
    INSERT INTO ratings (note_id, user_id, rating, comment)
    VALUES (?, ?, ?, ?)
    ON CONFLICT(note_id, user_id) DO UPDATE SET
      rating = excluded.rating,
      comment = excluded.comment,
      updated_at = datetime('now')
  `).run(note.id, req.user.id, ratingVal, comment);

  req.flash('success', 'Rating submitted.');
  res.redirect(`/notes/${note.id}`);
});

// POST /notes/:id/attachments/:attachmentId/delete
router.post('/:id/attachments/:attachmentId/delete', ensureAuthenticated, (req, res) => {
  const db = getDb();
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);

  if (!note || (note.user_id !== req.user.id && req.user.role !== 'admin')) {
    return res.status(403).render('error', {
      title: 'Forbidden', message: 'Unauthorized.', status: 403
    });
  }

  const attachment = db.prepare(
    'SELECT * FROM attachments WHERE id = ? AND note_id = ?'
  ).get(req.params.attachmentId, note.id);

  if (!attachment) {
    return res.status(404).render('error', {
      title: 'Not Found', message: 'Attachment not found.', status: 404
    });
  }

  const filePath = path.join(__dirname, '..', 'uploads', attachment.stored_filename);
  if (fs.existsSync(filePath)) fs.unlinkSync(filePath);

  db.prepare('DELETE FROM attachments WHERE id = ?').run(attachment.id);
  req.flash('success', 'Attachment deleted.');
  res.redirect(`/notes/${note.id}/edit`);
});

module.exports = router;
