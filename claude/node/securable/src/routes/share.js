'use strict';

const express = require('express');
const path    = require('path');

const db     = require('../config/db');
const logger = require('../config/logger');
const { UPLOAD_DIR } = require('../middleware/upload');

const router = express.Router();

// ─────────────────────────────────────────────────────────────────────────────
// GET /share/:token  — publicly accessible view via share link (no auth needed)
// ─────────────────────────────────────────────────────────────────────────────
router.get('/:token', (req, res) => {
  const { token } = req.params;

  const shareLink = db.prepare('SELECT * FROM share_links WHERE token = ?').get(token);
  if (!shareLink) {
    return res.status(404).render('error', { title: 'Not Found', message: 'Share link not found or has been revoked.' });
  }

  const note = db.prepare(
    `SELECT n.*, u.username,
            COALESCE(ROUND(AVG(r.stars),1), 0) AS avg_rating,
            COUNT(DISTINCT r.id) AS rating_count
     FROM notes n
     JOIN users u ON u.id = n.user_id
     LEFT JOIN ratings r ON r.note_id = n.id
     WHERE n.id = ?
     GROUP BY n.id`
  ).get(shareLink.note_id);

  if (!note) {
    return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.' });
  }

  const attachments = db.prepare(
    'SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at'
  ).all(note.id);

  const ratings = db.prepare(
    `SELECT r.*, u.username FROM ratings r JOIN users u ON u.id = r.user_id
     WHERE r.note_id = ? ORDER BY r.updated_at DESC`
  ).all(note.id);

  res.render('share-view', {
    title: note.title,
    note, attachments, ratings, token
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// GET /share/:token/attachments/:attachId — download via share link
// ─────────────────────────────────────────────────────────────────────────────
router.get('/:token/attachments/:attachId', (req, res) => {
  const { token } = req.params;
  const attachId  = parseInt(req.params.attachId, 10);

  const shareLink = db.prepare('SELECT * FROM share_links WHERE token = ?').get(token);
  if (!shareLink) return res.status(404).render('error', { title: 'Not Found', message: 'Share link not found.' });

  const attachment = db.prepare(
    'SELECT * FROM attachments WHERE id = ? AND note_id = ?'
  ).get(attachId, shareLink.note_id);

  if (!attachment) return res.status(404).render('error', { title: 'Not Found', message: 'Attachment not found.' });

  const filePath = path.join(UPLOAD_DIR, path.basename(attachment.stored_name));
  res.download(filePath, attachment.original_name, (err) => {
    if (err) {
      logger.error('Share attachment download error', { err: err.message, attachId, token });
      if (!res.headersSent) res.status(404).render('error', { title: 'Not Found', message: 'File not found.' });
    }
  });
});

module.exports = router;
