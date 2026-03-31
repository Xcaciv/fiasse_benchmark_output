'use strict';

const express = require('express');
const { body, query, param } = require('express-validator');
const noteService = require('../services/noteService');
const shareLinkService = require('../services/shareLinkService');
const ratingService = require('../services/ratingService');
const { handleValidationErrors } = require('../middleware/validate');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// All notes routes require authentication
router.use(requireAuth);

// GET /notes — list current user's notes
router.get('/', async (req, res, next) => {
  try {
    const notes = await noteService.getUserNotes(req.user.id);
    res.render('notes/index', { title: 'My Notes', notes });
  } catch (err) {
    next(err);
  }
});

// GET /notes/search
router.get(
  '/search',
  [query('q').trim().isLength({ min: 1, max: 200 }).withMessage('Search query required (max 200 chars).')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { q } = req.query;
      const results = await noteService.searchNotes(q, req.user.id);
      res.render('notes/search', { title: 'Search Results', results, query: q });
    } catch (err) {
      next(err);
    }
  }
);

// GET /notes/top-rated
router.get('/top-rated', async (req, res, next) => {
  try {
    const notes = await noteService.getTopRatedNotes(3, 20);
    res.render('notes/top-rated', { title: 'Top Rated Notes', notes });
  } catch (err) {
    next(err);
  }
});

// GET /notes/new
router.get('/new', (req, res) => {
  res.render('notes/create', { title: 'New Note' });
});

// POST /notes
router.post(
  '/',
  [
    body('title').trim().notEmpty().isLength({ max: 255 }).withMessage('Title is required (max 255 chars).'),
    body('content').trim().notEmpty().withMessage('Content is required.'),
    body('isPublic').optional().isIn(['on', 'off', '1', '0', 'true', 'false', '']).withMessage('Invalid visibility value.'),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { title, content, isPublic } = req.body;
      const note = await noteService.createNote({
        userId: req.user.id, // Derived Integrity: userId from session, never from body
        title,
        content,
        isPublic: isPublic === 'on' || isPublic === '1' || isPublic === 'true',
      });
      req.flash('success', 'Note created.');
      res.redirect(`/notes/${note.id}`);
    } catch (err) {
      next(err);
    }
  }
);

// GET /notes/:id
router.get(
  '/:id',
  [param('id').isUUID().withMessage('Invalid note ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const note = await noteService.getNoteById(req.params.id);
      if (!note) return res.status(404).render('error', { title: 'Not Found', status: 404, message: 'Note not found.' });
      if (!note.isPublic && note.userId !== req.user.id) {
        return res.status(403).render('error', { title: 'Access Denied', status: 403, message: 'You do not have access to this note.' });
      }
      const isOwner = note.userId === req.user.id;
      let shareLink = null;
      if (isOwner) {
        shareLink = await shareLinkService.getOrCreateShareLink(note.id, req.user.id).catch(() => null);
      }
      res.render('notes/view', { title: note.title, note, isOwner, shareLink });
    } catch (err) {
      next(err);
    }
  }
);

// GET /notes/:id/edit
router.get(
  '/:id/edit',
  [param('id').isUUID().withMessage('Invalid note ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const note = await noteService.assertOwnership(req.params.id, req.user.id);
      res.render('notes/edit', { title: 'Edit Note', note });
    } catch (err) {
      if (err.status === 403) {
        req.flash('error', 'Access denied.');
        return res.redirect('/notes');
      }
      next(err);
    }
  }
);

// POST /notes/:id/edit (method-override for PUT)
router.post(
  '/:id/edit',
  [
    param('id').isUUID().withMessage('Invalid note ID.'),
    body('title').trim().notEmpty().isLength({ max: 255 }).withMessage('Title is required (max 255 chars).'),
    body('content').trim().notEmpty().withMessage('Content is required.'),
    body('isPublic').optional().isIn(['on', 'off', '1', '0', 'true', 'false', '']).withMessage('Invalid visibility value.'),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { title, content, isPublic } = req.body;
      await noteService.updateNote(req.params.id, req.user.id, {
        title,
        content,
        isPublic: isPublic === 'on' || isPublic === '1' || isPublic === 'true',
      });
      req.flash('success', 'Note updated.');
      res.redirect(`/notes/${req.params.id}`);
    } catch (err) {
      if (err.status === 403) {
        req.flash('error', 'Access denied.');
        return res.redirect('/notes');
      }
      next(err);
    }
  }
);

// POST /notes/:id/delete
router.post(
  '/:id/delete',
  [param('id').isUUID().withMessage('Invalid note ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      await noteService.deleteNote(req.params.id, req.user.id, req.user.role === 'admin');
      req.flash('success', 'Note deleted.');
      res.redirect('/notes');
    } catch (err) {
      if (err.status === 403) {
        req.flash('error', 'Access denied.');
        return res.redirect('/notes');
      }
      next(err);
    }
  }
);

// POST /notes/:id/share/regenerate
router.post(
  '/:id/share/regenerate',
  [param('id').isUUID().withMessage('Invalid note ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      await shareLinkService.regenerateShareLink(req.params.id, req.user.id);
      req.flash('success', 'Share link regenerated.');
      res.redirect(`/notes/${req.params.id}`);
    } catch (err) {
      if (err.status === 403) {
        req.flash('error', 'Access denied.');
        return res.redirect('/notes');
      }
      next(err);
    }
  }
);

// POST /notes/:id/share/revoke
router.post(
  '/:id/share/revoke',
  [param('id').isUUID().withMessage('Invalid note ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      await shareLinkService.revokeShareLinks(req.params.id, req.user.id);
      req.flash('success', 'Share link revoked.');
      res.redirect(`/notes/${req.params.id}`);
    } catch (err) {
      if (err.status === 403) {
        req.flash('error', 'Access denied.');
        return res.redirect('/notes');
      }
      next(err);
    }
  }
);

// POST /notes/:id/rate
router.post(
  '/:id/rate',
  [
    param('id').isUUID().withMessage('Invalid note ID.'),
    body('value').isInt({ min: 1, max: 5 }).withMessage('Rating must be between 1 and 5.'),
    body('comment').optional().trim().isLength({ max: 1000 }).withMessage('Comment max 1000 characters.'),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { value, comment } = req.body;
      await ratingService.upsertRating({
        noteId: req.params.id,
        userId: req.user.id, // Derived Integrity
        value,
        comment,
      });
      req.flash('success', 'Rating saved.');
      res.redirect(`/notes/${req.params.id}`);
    } catch (err) {
      if (err.status === 403 || err.status === 404) {
        req.flash('error', err.message);
        return res.redirect(`/notes/${req.params.id}`);
      }
      next(err);
    }
  }
);

module.exports = router;
