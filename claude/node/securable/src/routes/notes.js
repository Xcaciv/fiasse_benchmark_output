'use strict';

const path    = require('path');
const fs      = require('fs');
const express = require('express');
const { body, validationResult } = require('express-validator');
const { v4: uuidv4 } = require('uuid');

const db     = require('../config/db');
const logger = require('../config/logger');
const { requireAuth } = require('../middleware/auth');
const { upload, UPLOAD_DIR } = require('../middleware/upload');

const router = express.Router();

// ── Helper: log activity ──────────────────────────────────────────────────
function logActivity(userId, action, details, ip) {
  try {
    db.prepare(`INSERT INTO activity_log (user_id, action, details, ip_address)
                VALUES (?, ?, ?, ?)`).run(userId || null, action, details || null, ip || null);
  } catch (_) { /* non-fatal */ }
}

// ── Helper: ownership check ───────────────────────────────────────────────
function getOwnedNote(noteId, userId) {
  return db.prepare('SELECT * FROM notes WHERE id = ? AND user_id = ?').get(noteId, userId);
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /notes  — list the authenticated user's notes
// ─────────────────────────────────────────────────────────────────────────────
router.get('/', requireAuth, (req, res) => {
  const notes = db.prepare(
    `SELECT n.*, u.username,
            COALESCE(ROUND(AVG(r.stars),1), 0) AS avg_rating,
            COUNT(DISTINCT r.id) AS rating_count
     FROM notes n
     JOIN users u ON u.id = n.user_id
     LEFT JOIN ratings r ON r.note_id = n.id
     WHERE n.user_id = ?
     GROUP BY n.id
     ORDER BY n.updated_at DESC`
  ).all(req.session.userId);

  res.render('notes/index', { title: 'My Notes', notes });
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /notes/top-rated
// ─────────────────────────────────────────────────────────────────────────────
router.get('/top-rated', (req, res) => {
  const notes = db.prepare(
    `SELECT n.*, u.username,
            ROUND(AVG(r.stars), 1) AS avg_rating,
            COUNT(r.id) AS rating_count
     FROM notes n
     JOIN users u ON u.id = n.user_id
     JOIN ratings r ON r.note_id = n.id
     WHERE n.is_public = 1
     GROUP BY n.id
     HAVING COUNT(r.id) >= 3
     ORDER BY avg_rating DESC, rating_count DESC
     LIMIT 50`
  ).all();

  res.render('top-rated', { title: 'Top Rated Notes', notes });
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /notes/create
// ─────────────────────────────────────────────────────────────────────────────
router.get('/create', requireAuth, (req, res) => {
  res.render('notes/create', { title: 'New Note', errors: [], old: {} });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /notes/create
// ─────────────────────────────────────────────────────────────────────────────
router.post('/create', requireAuth, upload.array('attachments', 5), [
  body('title').trim().notEmpty().withMessage('Title is required').isLength({ max: 255 }),
  body('content').trim().notEmpty().withMessage('Content is required')
], (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    // Remove any uploaded files on validation failure
    (req.files || []).forEach(f => fs.unlink(f.path, () => {}));
    return res.render('notes/create', {
      title: 'New Note',
      errors: errors.array(),
      old: { title: req.body.title, content: req.body.content }
    });
  }

  const { title, content, is_public } = req.body;
  const isPublic = is_public === 'on' ? 1 : 0;

  const result = db.prepare(
    'INSERT INTO notes (user_id, title, content, is_public) VALUES (?, ?, ?, ?)'
  ).run(req.session.userId, title.trim(), content.trim(), isPublic);

  const noteId = result.lastInsertRowid;

  // Save attachments
  (req.files || []).forEach(file => {
    db.prepare(
      'INSERT INTO attachments (note_id, original_name, stored_name, mime_type, size) VALUES (?, ?, ?, ?, ?)'
    ).run(noteId, file.originalname, file.filename, file.mimetype, file.size);
  });

  logActivity(req.session.userId, 'note_created', `noteId=${noteId}`, req.ip);
  logger.info('Note created', { userId: req.session.userId, noteId });

  res.redirect(`/notes/${noteId}`);
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /notes/:id  — view a note
// ─────────────────────────────────────────────────────────────────────────────
router.get('/:id', (req, res) => {
  const noteId = parseInt(req.params.id, 10);
  if (!noteId) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  const note = db.prepare(
    `SELECT n.*, u.username,
            COALESCE(ROUND(AVG(r.stars),1), 0) AS avg_rating,
            COUNT(DISTINCT r.id) AS rating_count
     FROM notes n
     JOIN users u ON u.id = n.user_id
     LEFT JOIN ratings r ON r.note_id = n.id
     WHERE n.id = ?
     GROUP BY n.id`
  ).get(noteId);

  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  const currentUserId = req.session.userId || null;
  const isOwner = currentUserId && note.user_id === currentUserId;

  // Access control: private notes visible only to owner
  if (!note.is_public && !isOwner) {
    return res.status(403).render('error', { title: 'Forbidden', message: 'This note is private.' });
  }

  const attachments = db.prepare('SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at').all(noteId);
  const ratings     = db.prepare(
    `SELECT r.*, u.username FROM ratings r JOIN users u ON u.id = r.user_id
     WHERE r.note_id = ? ORDER BY r.updated_at DESC`
  ).all(noteId);

  const shareLink = db.prepare('SELECT * FROM share_links WHERE note_id = ?').get(noteId);

  // Current user's rating if any
  const myRating = currentUserId
    ? db.prepare('SELECT * FROM ratings WHERE note_id = ? AND user_id = ?').get(noteId, currentUserId)
    : null;

  res.render('notes/view', {
    title: note.title,
    note, isOwner, attachments, ratings, shareLink, myRating
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /notes/:id/edit
// ─────────────────────────────────────────────────────────────────────────────
router.get('/:id/edit', requireAuth, (req, res) => {
  const noteId = parseInt(req.params.id, 10);
  const note = getOwnedNote(noteId, req.session.userId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  const attachments = db.prepare('SELECT * FROM attachments WHERE note_id = ?').all(noteId);
  res.render('notes/edit', { title: 'Edit Note', note, attachments, errors: [] });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /notes/:id/edit
// ─────────────────────────────────────────────────────────────────────────────
router.post('/:id/edit', requireAuth, upload.array('attachments', 5), [
  body('title').trim().notEmpty().withMessage('Title is required').isLength({ max: 255 }),
  body('content').trim().notEmpty().withMessage('Content is required')
], (req, res) => {
  const noteId = parseInt(req.params.id, 10);
  const note = getOwnedNote(noteId, req.session.userId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    (req.files || []).forEach(f => fs.unlink(f.path, () => {}));
    const attachments = db.prepare('SELECT * FROM attachments WHERE note_id = ?').all(noteId);
    return res.render('notes/edit', { title: 'Edit Note', note, attachments, errors: errors.array() });
  }

  const { title, content, is_public } = req.body;
  const isPublic = is_public === 'on' ? 1 : 0;

  db.prepare(
    `UPDATE notes SET title = ?, content = ?, is_public = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?`
  ).run(title.trim(), content.trim(), isPublic, noteId);

  // Save new attachments
  (req.files || []).forEach(file => {
    db.prepare(
      'INSERT INTO attachments (note_id, original_name, stored_name, mime_type, size) VALUES (?, ?, ?, ?, ?)'
    ).run(noteId, file.originalname, file.filename, file.mimetype, file.size);
  });

  logActivity(req.session.userId, 'note_updated', `noteId=${noteId}`, req.ip);
  res.redirect(`/notes/${noteId}`);
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /notes/:id/delete
// ─────────────────────────────────────────────────────────────────────────────
router.post('/:id/delete', requireAuth, (req, res) => {
  const noteId = parseInt(req.params.id, 10);

  // Admins can delete any note; regular users only their own
  const isAdmin = req.session.userRole === 'admin';
  const note = isAdmin
    ? db.prepare('SELECT * FROM notes WHERE id = ?').get(noteId)
    : getOwnedNote(noteId, req.session.userId);

  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  // Remove stored attachment files
  const attachments = db.prepare('SELECT stored_name FROM attachments WHERE note_id = ?').all(noteId);
  attachments.forEach(att => {
    const filePath = path.join(UPLOAD_DIR, att.stored_name);
    fs.unlink(filePath, () => {});
  });

  // Cascading delete handles DB rows
  db.prepare('DELETE FROM notes WHERE id = ?').run(noteId);

  logActivity(req.session.userId, 'note_deleted', `noteId=${noteId}`, req.ip);
  logger.info('Note deleted', { userId: req.session.userId, noteId });

  res.redirect('/notes');
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /notes/:id/share  — manage share link
// ─────────────────────────────────────────────────────────────────────────────
router.get('/:id/share', requireAuth, (req, res) => {
  const noteId = parseInt(req.params.id, 10);
  const note = getOwnedNote(noteId, req.session.userId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  const shareLink = db.prepare('SELECT * FROM share_links WHERE note_id = ?').get(noteId);
  const baseUrl   = process.env.APP_BASE_URL || `http://localhost:${process.env.PORT || 3000}`;

  res.render('notes/share', { title: 'Share Note', note, shareLink, baseUrl });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /notes/:id/share/generate
// ─────────────────────────────────────────────────────────────────────────────
router.post('/:id/share/generate', requireAuth, (req, res) => {
  const noteId = parseInt(req.params.id, 10);
  const note = getOwnedNote(noteId, req.session.userId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  const token = uuidv4();
  // Upsert: replace any existing share link for this note
  db.prepare('INSERT OR REPLACE INTO share_links (note_id, token) VALUES (?, ?)').run(noteId, token);

  logActivity(req.session.userId, 'share_link_generated', `noteId=${noteId}`, req.ip);
  res.redirect(`/notes/${noteId}/share`);
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /notes/:id/share/revoke
// ─────────────────────────────────────────────────────────────────────────────
router.post('/:id/share/revoke', requireAuth, (req, res) => {
  const noteId = parseInt(req.params.id, 10);
  const note = getOwnedNote(noteId, req.session.userId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  db.prepare('DELETE FROM share_links WHERE note_id = ?').run(noteId);
  logActivity(req.session.userId, 'share_link_revoked', `noteId=${noteId}`, req.ip);
  res.redirect(`/notes/${noteId}/share`);
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /notes/:id/rate
// ─────────────────────────────────────────────────────────────────────────────
router.post('/:id/rate', requireAuth, [
  body('stars').isInt({ min: 1, max: 5 }).withMessage('Rating must be 1–5 stars'),
  body('comment').optional().trim().isLength({ max: 1000 })
], (req, res) => {
  const noteId = parseInt(req.params.id, 10);
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(noteId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  // Owners cannot rate their own notes
  if (note.user_id === req.session.userId) {
    req.session.flash = { error: 'You cannot rate your own note.' };
    return res.redirect(`/notes/${noteId}`);
  }

  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.session.flash = { error: errors.array()[0].msg };
    return res.redirect(`/notes/${noteId}`);
  }

  const { stars, comment } = req.body;

  db.prepare(`
    INSERT INTO ratings (note_id, user_id, stars, comment)
    VALUES (?, ?, ?, ?)
    ON CONFLICT(note_id, user_id)
    DO UPDATE SET stars = excluded.stars, comment = excluded.comment, updated_at = CURRENT_TIMESTAMP
  `).run(noteId, req.session.userId, parseInt(stars, 10), comment || null);

  res.redirect(`/notes/${noteId}`);
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /notes/:id/attachments/:attachId  — download attachment
// ─────────────────────────────────────────────────────────────────────────────
router.get('/:id/attachments/:attachId', (req, res) => {
  const noteId    = parseInt(req.params.id, 10);
  const attachId  = parseInt(req.params.attachId, 10);

  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(noteId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Not found.' });

  const currentUserId = req.session.userId || null;
  const isOwner = currentUserId && note.user_id === currentUserId;
  if (!note.is_public && !isOwner) {
    return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.' });
  }

  const attachment = db.prepare(
    'SELECT * FROM attachments WHERE id = ? AND note_id = ?'
  ).get(attachId, noteId);
  if (!attachment) return res.status(404).render('error', { title: 'Not Found', message: 'Attachment not found.' });

  // Prevent path traversal: only serve files whose stored name is a UUID+extension
  const filePath = path.join(UPLOAD_DIR, path.basename(attachment.stored_name));
  res.download(filePath, attachment.original_name, (err) => {
    if (err) {
      logger.error('Attachment download error', { err: err.message, attachId });
      if (!res.headersSent) res.status(404).render('error', { title: 'Not Found', message: 'File not found.' });
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /notes/:id/attachments/:attachId/delete
// ─────────────────────────────────────────────────────────────────────────────
router.post('/:id/attachments/:attachId/delete', requireAuth, (req, res) => {
  const noteId   = parseInt(req.params.id, 10);
  const attachId = parseInt(req.params.attachId, 10);

  const note = getOwnedNote(noteId, req.session.userId);
  if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });

  const attachment = db.prepare(
    'SELECT * FROM attachments WHERE id = ? AND note_id = ?'
  ).get(attachId, noteId);
  if (!attachment) return res.status(404).render('error', { title: 'Not Found', message: 'Attachment not found.' });

  fs.unlink(path.join(UPLOAD_DIR, attachment.stored_name), () => {});
  db.prepare('DELETE FROM attachments WHERE id = ?').run(attachId);

  res.redirect(`/notes/${noteId}/edit`);
});

module.exports = router;
