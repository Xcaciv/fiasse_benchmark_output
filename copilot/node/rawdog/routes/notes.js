const express = require('express');
const router = express.Router();
const { Op, fn, col, literal } = require('sequelize');
const { Note, User, Attachment, Rating, ShareLink } = require('../models');
const { ensureAuthenticated, ensureNoteOwner } = require('../middleware/auth');
const upload = require('../middleware/upload');
const { body, validationResult } = require('express-validator');
const fs = require('fs');
const path = require('path');
const { v4: uuidv4 } = require('uuid');

// Middleware to load note and set res.locals.note
async function loadNote(req, res, next) {
  try {
    const note = await Note.findByPk(req.params.id, {
      include: [
        { model: User, as: 'author', attributes: ['id', 'username'] },
        { model: Attachment },
        {
          model: Rating,
          include: [{ model: User, as: 'rater', attributes: ['username'] }],
          order: [['createdAt', 'DESC']],
        },
      ],
    });
    if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', user: req.user || null });
    res.locals.note = note;
    next();
  } catch (err) {
    next(err);
  }
}

// GET /notes — list my notes
router.get('/', ensureAuthenticated, async (req, res, next) => {
  try {
    const notes = await Note.findAll({
      where: { userId: req.user.id },
      order: [['updatedAt', 'DESC']],
    });
    res.render('notes/index', { title: 'My Notes', user: req.user, notes, messages: req.flash() });
  } catch (err) {
    next(err);
  }
});

// GET /notes/search
router.get('/search', ensureAuthenticated, async (req, res, next) => {
  const q = (req.query.q || '').trim();
  try {
    let notes = [];
    if (q) {
      notes = await Note.findAll({
        where: {
          [Op.and]: [
            {
              [Op.or]: [
                { title: { [Op.like]: `%${q}%` } },
                { content: { [Op.like]: `%${q}%` } },
              ],
            },
            {
              [Op.or]: [
                { userId: req.user.id },
                { visibility: 'public' },
              ],
            },
          ],
        },
        include: [{ model: User, as: 'author', attributes: ['username'] }],
        order: [['createdAt', 'DESC']],
      });
    }
    res.render('notes/search', { title: 'Search Notes', user: req.user, notes, q, messages: req.flash() });
  } catch (err) {
    next(err);
  }
});

// GET /notes/top-rated
router.get('/top-rated', async (req, res, next) => {
  try {
    const topNotes = await Note.findAll({
      where: { visibility: 'public' },
      include: [
        { model: User, as: 'author', attributes: ['username'] },
        { model: Rating, attributes: [] },
      ],
      attributes: {
        include: [
          [fn('AVG', col('Ratings.value')), 'avgRating'],
          [fn('COUNT', col('Ratings.id')), 'ratingCount'],
        ],
      },
      group: ['Note.id', 'author.id'],
      having: literal('COUNT("Ratings"."id") >= 3'),
      order: [[literal('avgRating'), 'DESC']],
    });
    res.render('top-rated', { title: 'Top Rated Notes', user: req.user || null, notes: topNotes, messages: req.flash() });
  } catch (err) {
    next(err);
  }
});

// GET /notes/create
router.get('/create', ensureAuthenticated, (req, res) => {
  res.render('notes/create', { title: 'Create Note', user: req.user, errors: [], messages: req.flash() });
});

// POST /notes
router.post('/', ensureAuthenticated, upload.array('attachments', 10), [
  body('title').trim().notEmpty().withMessage('Title is required.'),
  body('content').trim().notEmpty().withMessage('Content is required.'),
], async (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    // Remove any uploaded files on validation error
    if (req.files) req.files.forEach(f => fs.unlink(f.path, () => {}));
    return res.render('notes/create', { title: 'Create Note', user: req.user, errors: errors.array(), messages: req.flash() });
  }
  try {
    const note = await Note.create({
      title: req.body.title.trim(),
      content: req.body.content.trim(),
      visibility: req.body.visibility === 'public' ? 'public' : 'private',
      userId: req.user.id,
    });
    if (req.files && req.files.length > 0) {
      for (const file of req.files) {
        await Attachment.create({
          noteId: note.id,
          originalFilename: file.originalname,
          storedFilename: file.filename,
          mimeType: file.mimetype,
          fileSize: file.size,
        });
      }
    }
    req.flash('success', 'Note created successfully.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// GET /notes/:id
router.get('/:id', loadNote, async (req, res, next) => {
  const note = res.locals.note;
  const canView =
    note.visibility === 'public' ||
    (req.isAuthenticated() && (note.userId === req.user.id || req.user.role === 'admin'));
  if (!canView) {
    return res.status(403).render('error', { title: 'Forbidden', message: 'This note is private.', user: req.user || null });
  }
  try {
    const shareLinks = req.isAuthenticated() && (note.userId === req.user.id || req.user.role === 'admin')
      ? await ShareLink.findAll({ where: { noteId: note.id } })
      : [];
    const avgRating = note.Ratings && note.Ratings.length > 0
      ? (note.Ratings.reduce((a, r) => a + r.value, 0) / note.Ratings.length).toFixed(1)
      : null;
    const userRating = req.isAuthenticated()
      ? note.Ratings.find(r => r.userId === req.user.id)
      : null;
    res.render('notes/view', {
      title: note.title,
      user: req.user || null,
      note,
      shareLinks,
      avgRating,
      userRating,
      messages: req.flash(),
    });
  } catch (err) {
    next(err);
  }
});

// GET /notes/:id/edit
router.get('/:id/edit', ensureAuthenticated, loadNote, ensureNoteOwner, (req, res) => {
  res.render('notes/edit', { title: 'Edit Note', user: req.user, note: res.locals.note, errors: [], messages: req.flash() });
});

// POST /notes/:id/edit
router.post('/:id/edit', ensureAuthenticated, loadNote, ensureNoteOwner, upload.array('attachments', 10), [
  body('title').trim().notEmpty().withMessage('Title is required.'),
  body('content').trim().notEmpty().withMessage('Content is required.'),
], async (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    if (req.files) req.files.forEach(f => fs.unlink(f.path, () => {}));
    return res.render('notes/edit', { title: 'Edit Note', user: req.user, note: res.locals.note, errors: errors.array(), messages: req.flash() });
  }
  const note = res.locals.note;
  try {
    await note.update({
      title: req.body.title.trim(),
      content: req.body.content.trim(),
      visibility: req.body.visibility === 'public' ? 'public' : 'private',
    });
    if (req.files && req.files.length > 0) {
      for (const file of req.files) {
        await Attachment.create({
          noteId: note.id,
          originalFilename: file.originalname,
          storedFilename: file.filename,
          mimeType: file.mimetype,
          fileSize: file.size,
        });
      }
    }
    req.flash('success', 'Note updated.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/delete
router.post('/:id/delete', ensureAuthenticated, loadNote, ensureNoteOwner, async (req, res, next) => {
  const note = res.locals.note;
  try {
    // Delete attachment files from disk
    const attachments = await Attachment.findAll({ where: { noteId: note.id } });
    for (const att of attachments) {
      const filePath = path.join(__dirname, '..', 'public', 'uploads', att.storedFilename);
      fs.unlink(filePath, () => {});
    }
    await note.destroy();
    req.flash('success', 'Note deleted.');
    res.redirect('/notes');
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/rate
router.post('/:id/rate', ensureAuthenticated, loadNote, async (req, res, next) => {
  const note = res.locals.note;
  const value = parseInt(req.body.value);
  if (!value || value < 1 || value > 5) {
    req.flash('error', 'Rating must be between 1 and 5.');
    return res.redirect(`/notes/${note.id}`);
  }
  try {
    await Rating.upsert({
      noteId: note.id,
      userId: req.user.id,
      value,
      comment: req.body.comment ? req.body.comment.trim() : null,
    });
    req.flash('success', 'Rating saved.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/share
router.post('/:id/share', ensureAuthenticated, loadNote, ensureNoteOwner, async (req, res, next) => {
  const note = res.locals.note;
  try {
    const existing = await ShareLink.findOne({ where: { noteId: note.id } });
    if (existing) {
      req.flash('info', 'Share link already exists.');
      return res.redirect(`/notes/${note.id}`);
    }
    await ShareLink.create({ noteId: note.id, token: uuidv4() });
    req.flash('success', 'Share link created.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/share/revoke
router.post('/:id/share/revoke', ensureAuthenticated, loadNote, ensureNoteOwner, async (req, res, next) => {
  const note = res.locals.note;
  try {
    await ShareLink.destroy({ where: { noteId: note.id } });
    req.flash('success', 'Share link revoked.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/attachments/:attId/delete
router.post('/:id/attachments/:attId/delete', ensureAuthenticated, loadNote, ensureNoteOwner, async (req, res, next) => {
  try {
    const att = await Attachment.findOne({ where: { id: req.params.attId, noteId: req.params.id } });
    if (att) {
      fs.unlink(path.join(__dirname, '..', 'public', 'uploads', att.storedFilename), () => {});
      await att.destroy();
    }
    req.flash('success', 'Attachment deleted.');
    res.redirect(`/notes/${req.params.id}/edit`);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
