const express = require('express');
const multer = require('multer');
const { v4: uuidv4 } = require('uuid');
const path = require('path');
const fs = require('fs');
const { body, validationResult } = require('express-validator');
const { db } = require('../db/database');
const { isAuthenticated, isNoteOwner } = require('../middleware/auth');

const router = express.Router();

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, '../../uploads');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueName = `${uuidv4()}${path.extname(file.originalname)}`;
    cb(null, uniqueName);
  }
});

const fileFilter = (req, file, cb) => {
  const allowedExtensions = ['.pdf', '.doc', '.docx', '.txt', '.png', '.jpg', '.jpeg'];
  const ext = path.extname(file.originalname).toLowerCase();
  
  if (allowedExtensions.includes(ext)) {
    cb(null, true);
  } else {
    cb(new Error('Invalid file type. Allowed: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG'), false);
  }
};

const upload = multer({ 
  storage,
  fileFilter,
  limits: { fileSize: 10 * 1024 * 1024 }
});

router.get('/', isAuthenticated, (req, res) => {
  const notes = db.prepare(`
    SELECT n.*, u.username as author,
      (SELECT AVG(rating) FROM ratings WHERE note_id = n.id) as avg_rating,
      (SELECT COUNT(*) FROM ratings WHERE note_id = n.id) as rating_count
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.user_id = ?
    ORDER BY n.updated_at DESC
  `).all(req.session.user.id);

  res.render('notes/index', { user: req.session.user, notes });
});

router.get('/new', isAuthenticated, (req, res) => {
  res.render('notes/create', { 
    user: req.session.user, 
    errors: [],
    formData: {} 
  });
});

router.post('/', isAuthenticated, [
  body('title').trim().notEmpty().withMessage('Title is required'),
  body('content').trim().notEmpty().withMessage('Content is required')
], (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('notes/create', { 
      user: req.session.user, 
      errors: errors.array(),
      formData: req.body
    });
  }

  const { title, content, isPublic } = req.body;
  const isPublicVal = isPublic === 'on' ? 1 : 0;

  try {
    const result = db.prepare(
      'INSERT INTO notes (user_id, title, content, is_public) VALUES (?, ?, ?, ?)'
    ).run(req.session.user.id, title, content, isPublicVal);

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      req.session.user.id,
      'note_created',
      `Created note: ${title}`
    );

    req.flash('success', 'Note created successfully');
    res.redirect(`/notes/${result.lastInsertRowid}`);
  } catch (error) {
    console.error('Note creation error:', error);
    res.render('notes/create', { 
      user: req.session.user, 
      errors: [{ msg: 'An error occurred while creating the note' }],
      formData: req.body
    });
  }
});

router.get('/:id', isAuthenticated, (req, res) => {
  const note = db.prepare(`
    SELECT n.*, u.username as author
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.id = ?
  `).get(req.params.id);

  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: req.session.user });
  }

  const isOwner = note.user_id === req.session.user.id;
  const isAdmin = req.session.user.role === 'admin';
  
  if (!isOwner && !isAdmin && !note.is_public) {
    return res.status(403).render('error', { message: 'Access denied', user: req.session.user });
  }

  const attachments = db.prepare('SELECT * FROM attachments WHERE note_id = ?').all(note.id);
  
  const ratings = db.prepare(`
    SELECT r.*, u.username 
    FROM ratings r
    JOIN users u ON r.user_id = u.id
    WHERE r.note_id = ?
    ORDER BY r.created_at DESC
  `).all(note.id);

  const userRating = db.prepare('SELECT * FROM ratings WHERE note_id = ? AND user_id = ?').get(note.id, req.session.user.id);

  const avgRating = db.prepare('SELECT AVG(rating) as avg, COUNT(*) as count FROM ratings WHERE note_id = ?').get(note.id);

  const shareLink = db.prepare('SELECT * FROM share_links WHERE note_id = ?').get(note.id);

  res.render('notes/view', { 
    user: req.session.user, 
    note, 
    attachments, 
    ratings,
    userRating,
    avgRating,
    shareLink,
    isOwner,
    errors: []
  });
});

router.get('/:id/edit', isAuthenticated, (req, res) => {
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);
  
  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: req.session.user });
  }

  if (note.user_id !== req.session.user.id && req.session.user.role !== 'admin') {
    return res.status(403).render('error', { message: 'Access denied', user: req.session.user });
  }

  res.render('notes/edit', { 
    user: req.session.user, 
    note,
    errors: [],
    formData: note
  });
});

router.post('/:id/edit', isAuthenticated, [
  body('title').trim().notEmpty().withMessage('Title is required'),
  body('content').trim().notEmpty().withMessage('Content is required')
], (req, res) => {
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);
  
  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: req.session.user });
  }

  if (note.user_id !== req.session.user.id && req.session.user.role !== 'admin') {
    return res.status(403).render('error', { message: 'Access denied', user: req.session.user });
  }

  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('notes/edit', { 
      user: req.session.user, 
      note,
      errors: errors.array(),
      formData: req.body
    });
  }

  const { title, content, isPublic } = req.body;
  const isPublicVal = isPublic === 'on' ? 1 : 0;

  try {
    db.prepare(`
      UPDATE notes 
      SET title = ?, content = ?, is_public = ?, updated_at = datetime('now')
      WHERE id = ?
    `).run(title, content, isPublicVal, req.params.id);

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      req.session.user.id,
      'note_updated',
      `Updated note: ${title}`
    );

    req.flash('success', 'Note updated successfully');
    res.redirect(`/notes/${req.params.id}`);
  } catch (error) {
    console.error('Note update error:', error);
    res.render('notes/edit', { 
      user: req.session.user, 
      note,
      errors: [{ msg: 'An error occurred while updating the note' }],
      formData: req.body
    });
  }
});

router.post('/:id/delete', isAuthenticated, (req, res) => {
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);
  
  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: req.session.user });
  }

  if (note.user_id !== req.session.user.id && req.session.user.role !== 'admin') {
    return res.status(403).render('error', { message: 'Access denied', user: req.session.user });
  }

  const attachments = db.prepare('SELECT stored_filename FROM attachments WHERE note_id = ?').all(req.params.id);
  
  for (const att of attachments) {
    const filePath = path.join(__dirname, '../../uploads', att.stored_filename);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  }

  db.prepare('DELETE FROM notes WHERE id = ?').run(req.params.id);

  db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
    req.session.user.id,
    'note_deleted',
    `Deleted note: ${note.title}`
  );

  req.flash('success', 'Note deleted successfully');
  res.redirect('/notes');
});

router.post('/:id/attachments', isAuthenticated, upload.single('attachment'), (req, res) => {
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);
  
  if (!note) {
    return res.status(404).json({ error: 'Note not found' });
  }

  if (note.user_id !== req.session.user.id && req.session.user.role !== 'admin') {
    return res.status(403).json({ error: 'Access denied' });
  }

  if (!req.file) {
    return res.status(400).json({ error: 'No file uploaded or invalid file type' });
  }

  try {
    db.prepare(`
      INSERT INTO attachments (note_id, original_filename, stored_filename, mime_type, file_size)
      VALUES (?, ?, ?, ?, ?)
    `).run(
      req.params.id,
      req.file.originalname,
      req.file.filename,
      req.file.mimetype,
      req.file.size
    );

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      req.session.user.id,
      'attachment_added',
      `Added attachment: ${req.file.originalname} to note: ${note.title}`
    );

    req.flash('success', 'File uploaded successfully');
    res.redirect(`/notes/${req.params.id}`);
  } catch (error) {
    console.error('Attachment upload error:', error);
    res.status(500).json({ error: 'An error occurred while uploading the file' });
  }
});

router.post('/:id/attachments/:attId/delete', isAuthenticated, (req, res) => {
  const attachment = db.prepare('SELECT * FROM attachments WHERE id = ? AND note_id = ?').get(req.params.attId, req.params.id);
  
  if (!attachment) {
    return res.status(404).json({ error: 'Attachment not found' });
  }

  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);
  
  if (note.user_id !== req.session.user.id && req.session.user.role !== 'admin') {
    return res.status(403).json({ error: 'Access denied' });
  }

  const filePath = path.join(__dirname, '../../uploads', attachment.stored_filename);
  if (fs.existsSync(filePath)) {
    fs.unlinkSync(filePath);
  }

  db.prepare('DELETE FROM attachments WHERE id = ?').run(attachment.id);

  db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
    req.session.user.id,
    'attachment_deleted',
    `Deleted attachment: ${attachment.original_filename}`
  );

  req.flash('success', 'Attachment deleted successfully');
  res.redirect(`/notes/${req.params.id}`);
});

router.get('/:id/share', isAuthenticated, (req, res) => {
  const note = db.prepare(`
    SELECT n.*, u.username as author
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.id = ?
  `).get(req.params.id);

  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: req.session.user });
  }

  if (note.user_id !== req.session.user.id && req.session.user.role !== 'admin') {
    return res.status(403).render('error', { message: 'Access denied', user: req.session.user });
  }

  let shareLink = db.prepare('SELECT * FROM share_links WHERE note_id = ?').get(note.id);

  res.render('notes/share', { 
    user: req.session.user, 
    note,
    shareLink,
    errors: []
  });
});

router.post('/:id/share', isAuthenticated, (req, res) => {
  const note = db.prepare('SELECT * FROM notes WHERE id = ?').get(req.params.id);
  
  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: req.session.user });
  }

  if (note.user_id !== req.session.user.id && req.session.user.role !== 'admin') {
    return res.status(403).render('error', { message: 'Access denied', user: req.session.user });
  }

  const { action } = req.body;

  if (action === 'regenerate') {
    db.prepare('DELETE FROM share_links WHERE note_id = ?').run(note.id);
    
    const newToken = uuidv4();
    db.prepare('INSERT INTO share_links (note_id, token) VALUES (?, ?)').run(note.id, newToken);

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      req.session.user.id,
      'share_link_regenerated',
      `Regenerated share link for note: ${note.title}`
    );

    req.flash('success', 'Share link regenerated');
  } else if (action === 'revoke') {
    db.prepare('DELETE FROM share_links WHERE note_id = ?').run(note.id);

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      req.session.user.id,
      'share_link_revoked',
      `Revoked share link for note: ${note.title}`
    );

    req.flash('success', 'Share link revoked');
  }

  res.redirect(`/notes/${note.id}/share`);
});

router.get('/share/:token', (req, res) => {
  const shareLink = db.prepare('SELECT * FROM share_links WHERE token = ?').get(req.params.token);
  
  if (!shareLink) {
    return res.status(404).render('error', { message: 'Share link not found or has been revoked', user: null });
  }

  const note = db.prepare(`
    SELECT n.*, u.username as author
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.id = ?
  `).get(shareLink.note_id);

  if (!note) {
    return res.status(404).render('error', { message: 'Note not found', user: null });
  }

  const attachments = db.prepare('SELECT * FROM attachments WHERE note_id = ?').all(note.id);
  
  const ratings = db.prepare(`
    SELECT r.*, u.username 
    FROM ratings r
    JOIN users u ON r.user_id = u.id
    WHERE r.note_id = ?
    ORDER BY r.created_at DESC
  `).all(note.id);

  const avgRating = db.prepare('SELECT AVG(rating) as avg, COUNT(*) as count FROM ratings WHERE note_id = ?').get(note.id);

  res.render('notes/shared-view', { 
    user: null, 
    note, 
    attachments, 
    ratings,
    avgRating,
    shareLink,
    errors: []
  });
});

router.post('/:id/rate', isAuthenticated, [
  body('rating').isInt({ min: 1, max: 5 }).withMessage('Rating must be between 1 and 5')
], (req, res) => {
  const note = db.prepare(`
    SELECT n.*, u.username as author
    FROM notes n
    JOIN users u ON n.user_id = u.id
    WHERE n.id = ?
  `).get(req.params.id);

  if (!note) {
    return res.status(404).json({ error: 'Note not found' });
  }

  const isOwner = note.user_id === req.session.user.id;
  
  if (!isOwner && !note.is_public) {
    return res.status(403).json({ error: 'Access denied' });
  }

  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ error: errors.array()[0].msg });
  }

  const { rating, comment } = req.body;

  try {
    const existingRating = db.prepare('SELECT * FROM ratings WHERE note_id = ? AND user_id = ?').get(note.id, req.session.user.id);
    
    if (existingRating) {
      db.prepare(`
        UPDATE ratings 
        SET rating = ?, comment = ?, updated_at = datetime('now')
        WHERE id = ?
      `).run(rating, comment || null, existingRating.id);
    } else {
      db.prepare(`
        INSERT INTO ratings (note_id, user_id, rating, comment)
        VALUES (?, ?, ?, ?)
      `).run(note.id, req.session.user.id, rating, comment || null);
    }

    db.prepare('INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)').run(
      req.session.user.id,
      'note_rated',
      `Rated note: ${note.title} with ${rating} stars`
    );

    req.flash('success', 'Rating submitted successfully');
    res.redirect(`/notes/${note.id}`);
  } catch (error) {
    console.error('Rating error:', error);
    res.status(500).json({ error: 'An error occurred while submitting rating' });
  }
});

module.exports = router;
