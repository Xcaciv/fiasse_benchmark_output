const express = require('express');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const { v4: uuidv4 } = require('uuid');
const { Op, fn, col, literal } = require('sequelize');
const { Note, Attachment, Rating, ShareLink, User, ActivityLog } = require('../models');
const { isAuthenticated } = require('../middleware/auth');

const router = express.Router();

const ALLOWED_MIMETYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'image/png',
  'image/jpeg',
];
const ALLOWED_EXTENSIONS = ['.pdf', '.doc', '.docx', '.txt', '.png', '.jpg', '.jpeg'];

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, path.join(__dirname, '..', 'uploads'));
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    cb(null, uuidv4() + ext);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(ext) || !ALLOWED_MIMETYPES.includes(file.mimetype)) {
      return cb(new Error('Invalid file type.'));
    }
    cb(null, true);
  },
});

// GET /notes/search — must be before /:id
router.get('/search', isAuthenticated, async (req, res) => {
  try {
    const { q } = req.query;
    let notes = [];
    if (q && q.trim()) {
      const keyword = q.trim();
      notes = await Note.findAll({
        where: {
          [Op.and]: [
            {
              [Op.or]: [
                { title: { [Op.like]: `%${keyword}%` } },
                { content: { [Op.like]: `%${keyword}%` } },
              ],
            },
            {
              [Op.or]: [
                { userId: req.session.userId },
                { visibility: 'public' },
              ],
            },
          ],
        },
        include: [{ model: User, attributes: ['username'] }],
        order: [['createdAt', 'DESC']],
      });
    }
    res.render('notes/search', { title: 'Search Notes', notes, q: q || '' });
  } catch (err) {
    console.error(err);
    req.flash('error', 'Search failed.');
    res.redirect('/notes');
  }
});

// GET /notes/top-rated — must be before /:id
router.get('/top-rated', isAuthenticated, async (req, res) => {
  try {
    const notes = await Note.findAll({
      where: { visibility: 'public' },
      include: [
        { model: User, attributes: ['username'] },
        { model: Rating, attributes: [] },
      ],
      attributes: {
        include: [
          [fn('AVG', col('Ratings.value')), 'avgRating'],
          [fn('COUNT', col('Ratings.id')), 'ratingCount'],
        ],
      },
      group: ['Note.id'],
      having: literal('COUNT("Ratings"."id") >= 3'),
      order: [[literal('avgRating'), 'DESC']],
    });
    res.render('notes/top-rated', { title: 'Top Rated Notes', notes });
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to load top-rated notes.');
    res.redirect('/notes');
  }
});

// GET /notes
router.get('/', isAuthenticated, async (req, res) => {
  try {
    const notes = await Note.findAll({
      where: { userId: req.session.userId },
      order: [['createdAt', 'DESC']],
    });
    res.render('notes/index', { title: 'My Notes', notes });
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to load notes.');
    res.redirect('/');
  }
});

// GET /notes/new
router.get('/new', isAuthenticated, (req, res) => {
  res.render('notes/create', { title: 'New Note' });
});

// POST /notes
router.post('/', isAuthenticated, async (req, res) => {
  try {
    const { title, content, visibility } = req.body;
    if (!title || !content) {
      req.flash('error', 'Title and content are required.');
      return res.redirect('/notes/new');
    }
    const note = await Note.create({
      title,
      content,
      visibility: visibility === 'public' ? 'public' : 'private',
      userId: req.session.userId,
    });
    req.flash('success', 'Note created.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to create note.');
    res.redirect('/notes/new');
  }
});

// GET /notes/:id
router.get('/:id', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id, {
      include: [
        { model: User, attributes: ['username'] },
        { model: Attachment },
        {
          model: Rating,
          include: [{ model: User, attributes: ['username'] }],
          order: [['createdAt', 'DESC']],
        },
        { model: ShareLink },
      ],
    });
    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.session.username || null });
    }
    const isOwner = note.userId === req.session.userId;
    if (!isOwner && note.visibility === 'private' && req.session.role !== 'admin') {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
    }
    const avgRating = note.Ratings.length
      ? (note.Ratings.reduce((s, r) => s + r.value, 0) / note.Ratings.length).toFixed(1)
      : null;
    const userRating = note.Ratings.find(r => r.userId === req.session.userId);
    res.render('notes/view', { title: note.title, note, isOwner, avgRating, userRating: userRating || null });
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to load note.');
    res.redirect('/notes');
  }
});

// GET /notes/:id/edit
router.get('/:id/edit', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.session.username || null });
    if (note.userId !== req.session.userId && req.session.role !== 'admin') {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
    }
    res.render('notes/edit', { title: 'Edit Note', note });
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to load note.');
    res.redirect('/notes');
  }
});

// POST /notes/:id (update)
router.post('/:id', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.session.username || null });
    if (note.userId !== req.session.userId && req.session.role !== 'admin') {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
    }
    const { title, content, visibility } = req.body;
    if (!title || !content) {
      req.flash('error', 'Title and content are required.');
      return res.redirect(`/notes/${note.id}/edit`);
    }
    await note.update({ title, content, visibility: visibility === 'public' ? 'public' : 'private' });
    req.flash('success', 'Note updated.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to update note.');
    res.redirect('/notes');
  }
});

// POST /notes/:id/delete
router.post('/:id/delete', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id, { include: [{ model: Attachment }] });
    if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.session.username || null });
    if (note.userId !== req.session.userId && req.session.role !== 'admin') {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
    }
    // Delete physical files
    for (const att of note.Attachments) {
      const filePath = path.join(__dirname, '..', 'uploads', att.storedName);
      if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
    }
    await ActivityLog.create({ userId: req.session.userId, action: 'note_delete', details: `Deleted note: ${note.title}` });
    await note.destroy();
    req.flash('success', 'Note deleted.');
    res.redirect('/notes');
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to delete note.');
    res.redirect('/notes');
  }
});

// POST /notes/:id/attachments
router.post('/:id/attachments', isAuthenticated, (req, res, next) => {
  upload.single('file')(req, res, async (err) => {
    if (err) {
      req.flash('error', err.message || 'Upload failed.');
      return res.redirect(`/notes/${req.params.id}`);
    }
    try {
      const note = await Note.findByPk(req.params.id);
      if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.session.username || null });
      if (note.userId !== req.session.userId) {
        return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
      }
      if (!req.file) {
        req.flash('error', 'No file uploaded.');
        return res.redirect(`/notes/${req.params.id}`);
      }
      await Attachment.create({
        noteId: note.id,
        originalName: req.file.originalname,
        storedName: req.file.filename,
        mimetype: req.file.mimetype,
        size: req.file.size,
      });
      req.flash('success', 'Attachment uploaded.');
      res.redirect(`/notes/${note.id}`);
    } catch (e) {
      console.error(e);
      req.flash('error', 'Upload failed.');
      res.redirect(`/notes/${req.params.id}`);
    }
  });
});

// POST /notes/:id/attachments/:attachId/delete
router.post('/:id/attachments/:attachId/delete', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note || note.userId !== req.session.userId) {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
    }
    const att = await Attachment.findByPk(req.params.attachId);
    if (!att || att.noteId !== note.id) {
      req.flash('error', 'Attachment not found.');
      return res.redirect(`/notes/${note.id}`);
    }
    const filePath = path.join(__dirname, '..', 'uploads', att.storedName);
    if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
    await att.destroy();
    req.flash('success', 'Attachment deleted.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to delete attachment.');
    res.redirect(`/notes/${req.params.id}`);
  }
});

// GET /notes/:id/attachments/:attachId/download
router.get('/:id/attachments/:attachId/download', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.session.username || null });
    const isOwner = note.userId === req.session.userId;
    if (!isOwner && note.visibility === 'private' && req.session.role !== 'admin') {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
    }
    const att = await Attachment.findByPk(req.params.attachId);
    if (!att || att.noteId !== note.id) {
      req.flash('error', 'Attachment not found.');
      return res.redirect(`/notes/${note.id}`);
    }
    const filePath = path.join(__dirname, '..', 'uploads', att.storedName);
    res.download(filePath, att.originalName);
  } catch (err) {
    console.error(err);
    req.flash('error', 'Download failed.');
    res.redirect(`/notes/${req.params.id}`);
  }
});

// POST /notes/:id/share
router.post('/:id/share', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note || note.userId !== req.session.userId) {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
    }
    const existing = await ShareLink.findOne({ where: { noteId: note.id } });
    if (existing) await existing.destroy();
    await ShareLink.create({ noteId: note.id, token: uuidv4() });
    req.flash('success', 'Share link generated.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to generate share link.');
    res.redirect(`/notes/${req.params.id}`);
  }
});

// POST /notes/:id/share/revoke
router.post('/:id/share/revoke', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note || note.userId !== req.session.userId) {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', user: req.session.username || null });
    }
    await ShareLink.destroy({ where: { noteId: note.id } });
    req.flash('success', 'Share link revoked.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to revoke share link.');
    res.redirect(`/notes/${req.params.id}`);
  }
});

// POST /notes/:id/ratings
router.post('/:id/ratings', isAuthenticated, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.session.username || null });
    const { value, comment } = req.body;
    const ratingValue = parseInt(value);
    if (!ratingValue || ratingValue < 1 || ratingValue > 5) {
      req.flash('error', 'Rating must be between 1 and 5.');
      return res.redirect(`/notes/${note.id}`);
    }
    const existing = await Rating.findOne({ where: { noteId: note.id, userId: req.session.userId } });
    if (existing) {
      await existing.update({ value: ratingValue, comment: comment || null });
    } else {
      await Rating.create({ noteId: note.id, userId: req.session.userId, value: ratingValue, comment: comment || null });
    }
    req.flash('success', 'Rating submitted.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to submit rating.');
    res.redirect(`/notes/${req.params.id}`);
  }
});

module.exports = router;
