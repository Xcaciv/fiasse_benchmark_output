'use strict';

const express = require('express');
const { requireAuth, optionalAuth } = require('../middleware/authenticate');
const { requireNoteOwnership, requireNoteOwnerOrAdmin } = require('../middleware/authorize');
const { noteValidation, ratingValidation } = require('../middleware/validate');
const { uploadLimiter } = require('../middleware/rateLimiter');
const noteService = require('../services/noteService');
const fileService = require('../services/fileService');
const ratingService = require('../services/ratingService');

const router = express.Router();
const upload = fileService.createMulterUpload();

// GET /notes/top-rated — public, optionalAuth
router.get('/top-rated', optionalAuth, async (req, res, next) => {
  try {
    const notes = await noteService.getTopRatedNotes();
    res.render('notes/top-rated', { title: 'Top Rated Notes', notes, user: req.user });
  } catch (err) {
    next(err);
  }
});

// GET /notes — list + search (requires auth)
router.get('/', requireAuth, async (req, res, next) => {
  try {
    const query = typeof req.query.q === 'string' ? req.query.q.trim() : '';
    const page = parseInt(req.query.page, 10) || 1;

    const result = query
      ? await noteService.searchNotes({ query, requestingUserId: req.session.userId, page })
      : await noteService.getUserNotes({ userId: req.session.userId, page });

    res.render('notes/index', { title: 'My Notes', ...result, query, user: res.locals.user });
  } catch (err) {
    next(err);
  }
});

// GET /notes/create
router.get('/create', requireAuth, (req, res) => {
  res.render('notes/create', { title: 'New Note', user: res.locals.user });
});

// POST /notes
router.post('/', requireAuth, noteValidation, async (req, res, next) => {
  try {
    const { title, content } = req.body;
    const note = await noteService.createNote({
      title, content, userId: req.session.userId, ipAddress: req.ip,
    });
    req.session.flash = { type: 'success', message: 'Note created!' };
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// GET /notes/:id — optionalAuth; enforces visibility check server-side
router.get('/:id', optionalAuth, async (req, res, next) => {
  try {
    const note = await noteService.getNoteById(req.params.id);
    if (!note) {
      return res.status(404).render('errors/404', { title: 'Note Not Found', user: req.user });
    }

    const userId = req.user ? req.user.id : null;
    const isOwner = Boolean(userId && note.userId === userId);

    // Trust boundary: server resolves visibility; client supplies nothing here
    if (!isOwner && note.visibility === 'private') {
      return res.status(403).render('errors/403', {
        title: 'Access Denied',
        message: 'This note is private.',
        user: req.user,
      });
    }

    const avgRating = await ratingService.getAverageRating(note.id);
    const userRating = userId ? note.Ratings.find((r) => r.userId === userId) : null;

    res.render('notes/view', {
      title: note.title,
      note,
      isOwner,
      avgRating,
      userRating: userRating || null,
      user: req.user,
      sharedView: false,
    });
  } catch (err) {
    next(err);
  }
});

// GET /notes/:id/edit
router.get('/:id/edit', requireAuth, requireNoteOwnership, (req, res) => {
  res.render('notes/edit', { title: 'Edit Note', note: req.note, user: res.locals.user });
});

// PUT /notes/:id
router.put('/:id', requireAuth, requireNoteOwnership, noteValidation, async (req, res, next) => {
  try {
    const { title, content, visibility } = req.body;
    // Derived Integrity Principle: only accept valid enum values
    const safeVisibility = ['public', 'private'].includes(visibility) ? visibility : 'private';

    await noteService.updateNote({
      id: req.params.id,
      userId: req.session.userId,
      isAdmin: req.session.role === 'admin',
      title,
      content,
      visibility: safeVisibility,
      ipAddress: req.ip,
    });

    req.session.flash = { type: 'success', message: 'Note updated.' };
    res.redirect(`/notes/${req.params.id}`);
  } catch (err) {
    next(err);
  }
});

// DELETE /notes/:id
router.delete('/:id', requireAuth, requireNoteOwnerOrAdmin, async (req, res, next) => {
  try {
    await noteService.deleteNote({
      id: req.params.id,
      userId: req.session.userId,
      isAdmin: req.session.role === 'admin',
      ipAddress: req.ip,
    });
    req.session.flash = { type: 'success', message: 'Note deleted.' };
    res.redirect('/notes');
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/share
router.post('/:id/share', requireAuth, requireNoteOwnership, async (req, res, next) => {
  try {
    await noteService.generateShareLink({
      noteId: req.params.id, userId: req.session.userId, ipAddress: req.ip,
    });
    req.session.flash = { type: 'success', message: 'Share link generated.' };
    res.redirect(`/notes/${req.params.id}`);
  } catch (err) {
    next(err);
  }
});

// DELETE /notes/:id/share
router.delete('/:id/share', requireAuth, requireNoteOwnership, async (req, res, next) => {
  try {
    await noteService.revokeShareLink({
      noteId: req.params.id, userId: req.session.userId, ipAddress: req.ip,
    });
    req.session.flash = { type: 'info', message: 'Share link revoked.' };
    res.redirect(`/notes/${req.params.id}`);
  } catch (err) {
    next(err);
  }
});

// POST /notes/:id/attachments
router.post(
  '/:id/attachments',
  requireAuth,
  uploadLimiter,
  requireNoteOwnership,
  upload.single('attachment'),
  async (req, res, next) => {
    try {
      if (!req.file) {
        req.session.flash = { type: 'error', message: 'No file was provided.' };
        return res.redirect(`/notes/${req.params.id}`);
      }

      await fileService.storeFile({
        file: req.file,
        noteId: req.params.id,
        userId: req.session.userId,
        ipAddress: req.ip,
      });

      req.session.flash = { type: 'success', message: 'File uploaded.' };
      res.redirect(`/notes/${req.params.id}`);
    } catch (err) {
      next(err);
    }
  }
);

// DELETE /notes/:id/attachments/:attachmentId
router.delete(
  '/:id/attachments/:attachmentId',
  requireAuth,
  requireNoteOwnership,
  async (req, res, next) => {
    try {
      await fileService.deleteFile({
        attachmentId: req.params.attachmentId,
        userId: req.session.userId,
        isAdmin: req.session.role === 'admin',
        ipAddress: req.ip,
      });
      req.session.flash = { type: 'success', message: 'Attachment removed.' };
      res.redirect(`/notes/${req.params.id}`);
    } catch (err) {
      next(err);
    }
  }
);

// POST /notes/:id/ratings
router.post('/:id/ratings', requireAuth, ratingValidation, async (req, res, next) => {
  try {
    const { stars, comment } = req.body;
    await ratingService.createOrUpdateRating({
      noteId: req.params.id,
      userId: req.session.userId,
      stars,
      comment,
      ipAddress: req.ip,
    });
    req.session.flash = { type: 'success', message: 'Rating submitted.' };
    res.redirect(`/notes/${req.params.id}`);
  } catch (err) {
    if (err.status === 403) {
      req.session.flash = { type: 'error', message: err.message };
      return res.redirect(`/notes/${req.params.id}`);
    }
    next(err);
  }
});

module.exports = router;
