const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const { v4: uuidv4 } = require('uuid');
const { body, validationResult } = require('express-validator');
const { Op } = require('sequelize');
const db = require('../models');
const { isAuthenticated } = require('../middleware/auth');
const upload = require('../middleware/upload');

const UPLOAD_DIR = path.resolve(process.env.UPLOAD_DIR || './uploads');

// IMPORTANT: Define specific routes BEFORE /:id to avoid Express catching them as IDs

// GET /notes - list user's notes
router.get('/', isAuthenticated, async (req, res, next) => {
  try {
    const notes = await db.Note.findAll({
      where: { userId: req.user.id },
      order: [['createdAt', 'DESC']],
      include: [
        { model: db.Rating, attributes: ['stars'] },
        { model: db.Attachment, attributes: ['id'] }
      ]
    });

    const notesWithStats = notes.map(note => {
      const ratings = note.Ratings || [];
      const avgRating = ratings.length > 0
        ? (ratings.reduce((sum, r) => sum + r.stars, 0) / ratings.length).toFixed(1)
        : null;
      return {
        ...note.toJSON(),
        avgRating,
        ratingCount: ratings.length,
        attachmentCount: (note.Attachments || []).length
      };
    });

    res.render('notes/index', { title: 'My Notes', notes: notesWithStats });
  } catch (err) {
    next(err);
  }
});

// GET /notes/create
router.get('/create', isAuthenticated, (req, res) => {
  res.render('notes/create', { title: 'Create Note', errors: [] });
});

// POST /notes/create
router.post('/create', isAuthenticated, upload.array('attachments', 10), [
  body('title').trim().isLength({ min: 1, max: 255 }).withMessage('Title is required and must be under 255 characters.'),
  body('content').trim().notEmpty().withMessage('Content is required.')
], async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      if (req.files) req.files.forEach(f => fs.unlink(f.path, () => {}));
      return res.render('notes/create', { title: 'Create Note', errors: errors.array() });
    }

    const { title, content, isPublic } = req.body;
    const note = await db.Note.create({
      title,
      content,
      isPublic: isPublic === 'on' || isPublic === 'true' || isPublic === '1',
      userId: req.user.id
    });

    if (req.files && req.files.length > 0) {
      const attachments = req.files.map(f => ({
        noteId: note.id,
        originalName: f.originalname,
        storedName: f.filename,
        mimeType: f.mimetype,
        size: f.size
      }));
      await db.Attachment.bulkCreate(attachments);
    }

    await db.AuditLog.create({
      userId: req.user.id,
      action: 'note_create',
      details: `Note created: "${title}" (id=${note.id})`,
      ipAddress: req.ip
    });

    req.flash('success_msg', 'Note created successfully.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    if (req.files) req.files.forEach(f => fs.unlink(f.path, () => {}));
    next(err);
  }
});

// GET /notes/search
router.get('/search', isAuthenticated, async (req, res, next) => {
  try {
    const { q } = req.query;
    let results = [];

    if (q && q.trim()) {
      const keyword = q.trim();
      results = await db.Note.findAll({
        where: {
          [Op.and]: [
            {
              [Op.or]: [
                { title: { [Op.like]: `%${keyword}%` } },
                { content: { [Op.like]: `%${keyword}%` } }
              ]
            },
            {
              [Op.or]: [
                { userId: req.user.id },
                { isPublic: true }
              ]
            }
          ]
        },
        include: [{ model: db.User, as: 'author', attributes: ['username'] }],
        order: [['createdAt', 'DESC']]
      });
    }

    res.render('notes/search', { title: 'Search Notes', results, q: q || '' });
  } catch (err) {
    next(err);
  }
});

// GET /notes/top-rated
router.get('/top-rated', async (req, res, next) => {
  try {
    const notes = await db.Note.findAll({
      where: { isPublic: true },
      include: [
        { model: db.Rating, attributes: ['stars'] },
        { model: db.User, as: 'author', attributes: ['username'] }
      ]
    });

    const rated = notes
      .map(note => {
        const ratings = note.Ratings || [];
        if (ratings.length < 3) return null;
        const avg = ratings.reduce((sum, r) => sum + r.stars, 0) / ratings.length;
        return { ...note.toJSON(), avgRating: avg.toFixed(2), ratingCount: ratings.length };
      })
      .filter(n => n !== null)
      .sort((a, b) => parseFloat(b.avgRating) - parseFloat(a.avgRating));

    res.render('notes/top-rated', { title: 'Top Rated Notes', notes: rated });
  } catch (err) {
    next(err);
  }
});

// GET /notes/:id
router.get('/:id', isAuthenticated, async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.id, {
      include: [
        { model: db.User, as: 'author', attributes: ['username', 'id'] },
        { model: db.Attachment },
        { model: db.Rating, include: [{ model: db.User, as: 'rater', attributes: ['username'] }] },
        { model: db.ShareLink }
      ]
    });

    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', statusCode: 404, message: 'Note not found.' });
    }

    if (note.userId !== req.user.id && !note.isPublic) {
      return res.status(403).render('error', { title: 'Access Denied', statusCode: 403, message: 'You do not have permission to view this note.' });
    }

    const ratings = note.Ratings || [];
    const avgRating = ratings.length > 0
      ? (ratings.reduce((sum, r) => sum + r.stars, 0) / ratings.length).toFixed(1)
      : null;

    const userRating = ratings.find(r => r.userId === req.user.id) || null;
    const isOwner = note.userId === req.user.id;

    res.render('notes/view', {
      title: note.title,
      note: note.toJSON(),
      ratings,
      avgRating,
      userRating,
      isOwner
    });
  } catch (err) {
    next(err);
  }
});

// GET /notes/:id/edit
router.get('/:id/edit', isAuthenticated, async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.id, {
      include: [{ model: db.Attachment }]
    });

    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', statusCode: 404, message: 'Note not found.' });
    }

    if (note.userId !== req.user.id && !req.user.isAdmin) {
      return res.status(403).render('error', { title: 'Access Denied', statusCode: 403, message: 'You do not have permission to edit this note.' });
    }

    res.render('notes/edit', { title: 'Edit Note', note: note.toJSON(), errors: [] });
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/edit
router.post('/:id/edit', isAuthenticated, upload.array('attachments', 10), [
  body('title').trim().isLength({ min: 1, max: 255 }).withMessage('Title is required and must be under 255 characters.'),
  body('content').trim().notEmpty().withMessage('Content is required.')
], async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.id, {
      include: [{ model: db.Attachment }]
    });

    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', statusCode: 404, message: 'Note not found.' });
    }

    if (note.userId !== req.user.id && !req.user.isAdmin) {
      return res.status(403).render('error', { title: 'Access Denied', statusCode: 403, message: 'You do not have permission to edit this note.' });
    }

    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      if (req.files) req.files.forEach(f => fs.unlink(f.path, () => {}));
      return res.render('notes/edit', { title: 'Edit Note', note: note.toJSON(), errors: errors.array() });
    }

    const { title, content, isPublic } = req.body;

    // Handle attachment deletions
    const deleteIds = req.body.deleteAttachments || [];
    const idsToDelete = Array.isArray(deleteIds) ? deleteIds : [deleteIds];
    if (idsToDelete.length > 0) {
      const toDelete = await db.Attachment.findAll({
        where: { id: idsToDelete, noteId: note.id }
      });
      for (const att of toDelete) {
        fs.unlink(path.join(UPLOAD_DIR, att.storedName), () => {});
        await att.destroy();
      }
    }

    await note.update({
      title,
      content,
      isPublic: isPublic === 'on' || isPublic === 'true' || isPublic === '1'
    });

    if (req.files && req.files.length > 0) {
      const attachments = req.files.map(f => ({
        noteId: note.id,
        originalName: f.originalname,
        storedName: f.filename,
        mimeType: f.mimetype,
        size: f.size
      }));
      await db.Attachment.bulkCreate(attachments);
    }

    await db.AuditLog.create({
      userId: req.user.id,
      action: 'note_edit',
      details: `Note updated: "${title}" (id=${note.id})`,
      ipAddress: req.ip
    });

    req.flash('success_msg', 'Note updated successfully.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    if (req.files) req.files.forEach(f => fs.unlink(f.path, () => {}));
    next(err);
  }
});

// GET /notes/:id/delete
router.get('/:id/delete', isAuthenticated, async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.id);

    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', statusCode: 404, message: 'Note not found.' });
    }

    if (note.userId !== req.user.id && !req.user.isAdmin) {
      return res.status(403).render('error', { title: 'Access Denied', statusCode: 403, message: 'You do not have permission to delete this note.' });
    }

    res.render('notes/delete', { title: 'Delete Note', note: note.toJSON() });
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/delete
router.post('/:id/delete', isAuthenticated, async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.id, {
      include: [{ model: db.Attachment }]
    });

    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', statusCode: 404, message: 'Note not found.' });
    }

    if (note.userId !== req.user.id && !req.user.isAdmin) {
      return res.status(403).render('error', { title: 'Access Denied', statusCode: 403, message: 'You do not have permission to delete this note.' });
    }

    const attachments = note.Attachments || [];
    for (const att of attachments) {
      fs.unlink(path.join(UPLOAD_DIR, att.storedName), () => {});
    }

    const noteTitle = note.title;
    const noteId = note.id;
    await note.destroy();

    await db.AuditLog.create({
      userId: req.user.id,
      action: 'note_delete',
      details: `Note deleted: "${noteTitle}" (id=${noteId})`,
      ipAddress: req.ip
    });

    req.flash('success_msg', 'Note deleted successfully.');
    res.redirect('/notes');
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/share - Create or get share link
router.post('/:id/share', isAuthenticated, async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.id);

    if (!note || note.userId !== req.user.id) {
      return res.status(403).render('error', { title: 'Access Denied', statusCode: 403, message: 'You do not have permission to share this note.' });
    }

    let shareLink = await db.ShareLink.findOne({ where: { noteId: note.id } });
    if (!shareLink) {
      shareLink = await db.ShareLink.create({
        noteId: note.id,
        token: uuidv4(),
        isActive: true
      });
    } else {
      await shareLink.update({ isActive: true });
    }

    req.flash('success_msg', 'Share link created/activated.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/revoke-share
router.post('/:id/revoke-share', isAuthenticated, async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.id);

    if (!note || note.userId !== req.user.id) {
      return res.status(403).render('error', { title: 'Access Denied', statusCode: 403, message: 'You do not have permission to revoke this share link.' });
    }

    const shareLink = await db.ShareLink.findOne({ where: { noteId: note.id } });
    if (shareLink) {
      await shareLink.update({ isActive: false });
    }

    req.flash('success_msg', 'Share link revoked.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/rate
router.post('/:id/rate', isAuthenticated, [
  body('stars').isInt({ min: 1, max: 5 }).withMessage('Rating must be between 1 and 5 stars.')
], async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.id);

    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', statusCode: 404, message: 'Note not found.' });
    }

    if (note.userId === req.user.id) {
      req.flash('error', 'You cannot rate your own note.');
      return res.redirect(`/notes/${note.id}`);
    }

    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      req.flash('error', errors.array()[0].msg);
      return res.redirect(`/notes/${note.id}`);
    }

    const { stars, comment } = req.body;

    const [rating, created] = await db.Rating.findOrCreate({
      where: { userId: req.user.id, noteId: note.id },
      defaults: { stars: parseInt(stars), comment: comment || null }
    });

    if (!created) {
      await rating.update({ stars: parseInt(stars), comment: comment || null });
    }

    req.flash('success_msg', created ? 'Rating submitted.' : 'Rating updated.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
