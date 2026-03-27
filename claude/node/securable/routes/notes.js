'use strict';

const express = require('express');
const { body, query } = require('express-validator');
const { Op } = require('sequelize');
const { Note, User, Attachment, Rating, ShareLink } = require('../models');
const { requireAuth } = require('../middleware/auth');
const { handleValidation } = require('../middleware/validate');
const audit = require('../services/auditService');
const fileStorage = require('../services/fileStorageService');
const security = require('../config/security');

const router = express.Router();

// ── Index: list own notes ──────────────────────────────────────────────────

router.get('/', requireAuth, async (req, res, next) => {
  try {
    const notes = await Note.findAll({
      where: { userId: req.user.id },
      order: [['createdAt', 'DESC']],
      attributes: ['id', 'title', 'isPublic', 'createdAt', 'updatedAt'],
    });
    res.render('notes/index', { title: 'My Notes', notes });
  } catch (err) {
    next(err);
  }
});

// ── Search ─────────────────────────────────────────────────────────────────

router.get('/search', requireAuth,
  query('q').trim().isLength({ max: 200 }).withMessage('Search query too long'),
  handleValidation,
  async (req, res, next) => {
    try {
      const q = (req.query.q || '').trim();
      if (!q) return res.render('notes/search', { title: 'Search', notes: [], q });

      const searchTerm = `%${q}%`;
      const notes = await Note.findAll({
        where: {
          [Op.and]: [
            { [Op.or]: [{ title: { [Op.like]: searchTerm } }, { content: { [Op.like]: searchTerm } }] },
            { [Op.or]: [{ userId: req.user.id }, { isPublic: true }] },
          ],
        },
        include: [{ model: User, as: 'owner', attributes: ['username'] }],
        order: [['createdAt', 'DESC']],
        attributes: ['id', 'title', 'content', 'isPublic', 'userId', 'createdAt'],
      });

      const results = notes.map((n) => ({
        id: n.id,
        title: n.title,
        excerpt: n.content.substring(0, 200),
        author: n.owner.username,
        createdAt: n.createdAt,
        isOwn: n.userId === req.user.id,
      }));

      return res.render('notes/search', { title: 'Search Results', notes: results, q });
    } catch (err) {
      return next(err);
    }
  }
);

// ── Top Rated ─────────────────────────────────────────────────────────────

router.get('/top-rated', requireAuth, async (req, res, next) => {
  try {
    const { sequelize } = require('../models');
    const notes = await Note.findAll({
      where: { isPublic: true },
      include: [
        { model: User, as: 'owner', attributes: ['username'] },
        { model: Rating, as: 'ratings', attributes: ['value'] },
      ],
      order: [['createdAt', 'DESC']],
    });

    const withStats = notes
      .map((n) => {
        const ratings = n.ratings || [];
        const count = ratings.length;
        const avg = count > 0 ? ratings.reduce((s, r) => s + r.value, 0) / count : 0;
        return {
          id: n.id,
          title: n.title,
          author: n.owner.username,
          avgRating: avg.toFixed(1),
          ratingCount: count,
          excerpt: n.content.substring(0, 200),
        };
      })
      .filter((n) => n.ratingCount >= 3)
      .sort((a, b) => b.avgRating - a.avgRating);

    res.render('notes/top-rated', { title: 'Top Rated Notes', notes: withStats });
  } catch (err) {
    next(err);
  }
});

// ── Create ─────────────────────────────────────────────────────────────────

const noteRules = [
  body('title').trim()
    .isLength({ min: 1, max: security.NOTE_TITLE_MAX_LENGTH })
    .withMessage(`Title required, max ${security.NOTE_TITLE_MAX_LENGTH} characters`),
  body('content').trim()
    .isLength({ min: 1, max: security.NOTE_CONTENT_MAX_LENGTH })
    .withMessage(`Content required, max ${security.NOTE_CONTENT_MAX_LENGTH} characters`),
  body('isPublic').toBoolean(),
];

router.get('/create', requireAuth, (req, res) => {
  res.render('notes/create', { title: 'New Note', csrfToken: req.csrfToken() });
});

router.post('/create', requireAuth, noteRules, handleValidation, async (req, res, next) => {
  try {
    const { title, content, isPublic } = req.body;
    const note = await Note.create({
      title: title.trim(),
      content: content.trim(),
      isPublic: Boolean(isPublic),
      userId: req.user.id,
    });
    await audit.record('note.create', req.user.id, { noteId: note.id }, req.ip);
    req.flash('success', 'Note created.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
});

// ── Details ────────────────────────────────────────────────────────────────

router.get('/:id', requireAuth, async (req, res, next) => {
  try {
    const note = await Note.findByPk(req.params.id, {
      include: [
        { model: User, as: 'owner', attributes: ['username'] },
        { model: Attachment, as: 'attachments' },
        {
          model: Rating,
          as: 'ratings',
          include: [{ model: User, as: 'rater', attributes: ['username'] }],
          order: [['createdAt', 'DESC']],
        },
      ],
    });

    if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', statusCode: 404 });

    // Access control: private notes visible only to owner
    const isOwner = note.userId === req.user.id;
    if (!note.isPublic && !isOwner && req.user.role !== 'admin') {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });
    }

    const ratings = note.ratings || [];
    const avgRating = ratings.length > 0
      ? (ratings.reduce((s, r) => s + r.value, 0) / ratings.length).toFixed(1)
      : null;

    const userRating = ratings.find((r) => r.userId === req.user.id);

    return res.render('notes/details', {
      title: note.title,
      note,
      isOwner,
      avgRating,
      userRating,
      csrfToken: req.csrfToken(),
    });
  } catch (err) {
    return next(err);
  }
});

// ── Edit ───────────────────────────────────────────────────────────────────

router.get('/:id/edit', requireAuth, async (req, res, next) => {
  try {
    const note = await findOwnedNote(req.params.id, req.user);
    if (!note) return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });
    res.render('notes/edit', { title: 'Edit Note', note, csrfToken: req.csrfToken() });
  } catch (err) {
    next(err);
  }
});

router.post('/:id/edit', requireAuth, noteRules, handleValidation, async (req, res, next) => {
  try {
    const note = await findOwnedNote(req.params.id, req.user);
    if (!note) return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });

    const { title, content, isPublic } = req.body;
    await note.update({ title: title.trim(), content: content.trim(), isPublic: Boolean(isPublic) });
    await audit.record('note.edit', req.user.id, { noteId: note.id }, req.ip);
    req.flash('success', 'Note updated.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
});

// ── Delete ─────────────────────────────────────────────────────────────────

router.get('/:id/delete', requireAuth, async (req, res, next) => {
  try {
    const note = await findOwnedNote(req.params.id, req.user);
    if (!note) return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });
    res.render('notes/delete', { title: 'Delete Note', note, csrfToken: req.csrfToken() });
  } catch (err) {
    next(err);
  }
});

router.post('/:id/delete', requireAuth, async (req, res, next) => {
  try {
    const note = await findOwnedNote(req.params.id, req.user);
    if (!note) return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });

    // Clean up attachment files from disk before DB delete
    const attachments = await Attachment.findAll({ where: { noteId: note.id } });
    for (const att of attachments) {
      await fileStorage.deleteFile(att.storedFilename);
    }

    await note.destroy(); // cascades to attachments, ratings, share_links in DB
    await audit.record('note.delete', req.user.id, { noteId: note.id }, req.ip);
    req.flash('success', 'Note deleted.');
    return res.redirect('/notes');
  } catch (err) {
    return next(err);
  }
});

// ── Ratings ────────────────────────────────────────────────────────────────

const ratingRules = [
  body('value').isInt({ min: 1, max: 5 }).withMessage('Rating must be 1–5'),
  body('comment').optional().trim().isLength({ max: 1000 }).withMessage('Comment max 1000 characters'),
];

router.post('/:id/rate', requireAuth, ratingRules, handleValidation, async (req, res, next) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) return res.status(404).json({ error: 'Note not found' });

    const [rating] = await Rating.upsert({
      noteId: note.id,
      userId: req.user.id,
      value: parseInt(req.body.value, 10),
      comment: req.body.comment?.trim() || null,
    });

    await audit.record('note.rate', req.user.id, { noteId: note.id, value: rating.value }, req.ip);
    req.flash('success', 'Rating saved.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
});

// ── Share Link Generation ─────────────────────────────────────────────────

router.post('/:id/share', requireAuth, async (req, res, next) => {
  try {
    const note = await findOwnedNote(req.params.id, req.user);
    if (!note) return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });

    const crypto = require('crypto');
    const token = crypto.randomBytes(32).toString('hex');
    await ShareLink.create({ noteId: note.id, token });
    await audit.record('note.share_link_created', req.user.id, { noteId: note.id }, req.ip);
    req.flash('success', `Share link created: ${req.protocol}://${req.get('host')}/share/${token}`);
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
});

router.post('/:id/share/revoke', requireAuth, async (req, res, next) => {
  try {
    const note = await findOwnedNote(req.params.id, req.user);
    if (!note) return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });

    await ShareLink.update({ isRevoked: true }, { where: { noteId: note.id } });
    await audit.record('note.share_link_revoked', req.user.id, { noteId: note.id }, req.ip);
    req.flash('success', 'All share links revoked.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
});

// ── Helpers ────────────────────────────────────────────────────────────────

async function findOwnedNote(noteId, user) {
  const note = await Note.findByPk(noteId);
  if (!note) return null;
  if (note.userId !== user.id && user.role !== 'admin') return null;
  return note;
}

module.exports = router;
