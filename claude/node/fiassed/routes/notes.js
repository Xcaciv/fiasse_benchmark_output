'use strict';

const express = require('express');
const { body, query, validationResult } = require('express-validator');
const { authenticate } = require('../middleware/authenticate');
const { noCacheForPrivate } = require('../middleware/security');
const { searchLimiter, topRatedLimiter } = require('../middleware/rateLimiter');
const noteService = require('../services/noteService');
const ratingService = require('../services/ratingService');
const shareLinkService = require('../services/shareLinkService');
const constants = require('../config/constants');

const router = express.Router();

// ─── List User Notes ──────────────────────────────────────────────────────────

router.get('/', authenticate, noCacheForPrivate, async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const { rows: notes, count } = await noteService.listUserNotes(
      req.currentUser.id, page, constants.PAGINATION.DEFAULT_PAGE_SIZE
    );

    res.render('notes/index', {
      title: 'My Notes',
      notes,
      count,
      page,
      pageSize: constants.PAGINATION.DEFAULT_PAGE_SIZE,
      currentUser: req.currentUser
    });
  } catch (err) {
    next(err);
  }
});

// ─── Create Note ─────────────────────────────────────────────────────────────

router.get('/create', authenticate, (req, res) => {
  res.render('notes/create', {
    title: 'Create Note',
    csrfToken: req.csrfToken(),
    errors: [],
    currentUser: req.currentUser
  });
});

router.post('/create', authenticate, [
  body('title').trim().isLength({ min: 1, max: 200 }),
  body('content').trim().isLength({ min: 1, max: 50000 }),
  body('visibility').isIn(['private', 'public'])
], async (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(422).render('notes/create', {
      title: 'Create Note',
      csrfToken: req.csrfToken(),
      errors: errors.array(),
      currentUser: req.currentUser
    });
  }

  try {
    const note = await noteService.createNote(
      req.currentUser.id,
      req.body,
      req.correlationId,
      req.ip
    );
    req.flash('success', 'Note created.');
    res.redirect(`/notes/${note.id}`);
  } catch (err) {
    next(err);
  }
});

// ─── Search ───────────────────────────────────────────────────────────────────

router.get('/search', authenticate, searchLimiter, [
  query('q').trim().isLength({ min: 1, max: 200 })
], async (req, res, next) => {
  try {
    const q = req.query.q || '';
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);

    if (!q) {
      return res.render('notes/search', {
        title: 'Search Notes',
        notes: [],
        count: 0,
        q: '',
        page: 1,
        pageSize: constants.PAGINATION.DEFAULT_PAGE_SIZE,
        currentUser: req.currentUser
      });
    }

    const { rows: notes, count } = await noteService.searchNotes(
      q, req.currentUser.id, page
    );

    res.render('notes/search', {
      title: `Search: ${q}`,
      notes,
      count,
      q,
      page,
      pageSize: constants.PAGINATION.DEFAULT_PAGE_SIZE,
      currentUser: req.currentUser
    });
  } catch (err) {
    next(err);
  }
});

// ─── Top Rated ────────────────────────────────────────────────────────────────

router.get('/top-rated', topRatedLimiter, async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const notes = await noteService.getTopRated(page, constants.PAGINATION.DEFAULT_PAGE_SIZE);

    res.render('notes/top-rated', {
      title: 'Top Rated Notes',
      notes,
      page,
      currentUser: req.currentUser || null
    });
  } catch (err) {
    next(err);
  }
});

// ─── View Note ───────────────────────────────────────────────────────────────

router.get('/:id', async (req, res, next) => {
  try {
    const userId = req.currentUser ? req.currentUser.id : null;
    const note = await noteService.getNoteById(req.params.id, userId);

    if (!note) {
      // Return 404 for both missing and unauthorized (no information leakage)
      return res.status(404).render('error', {
        title: 'Not Found',
        message: 'The requested note was not found.',
        correlationId: req.correlationId,
        currentUser: req.currentUser || null
      });
    }

    const { avg, count } = await ratingService.getAggregateRating(note.id);

    // Share links shown only to owner
    let shareLinks = [];
    if (req.currentUser && note.ownerId === req.currentUser.id) {
      shareLinks = await shareLinkService.getLinksForNote(note.id, req.currentUser.id) || [];
    }

    const headers = { 'Cache-Control': 'no-store', 'Pragma': 'no-cache' };
    if (note.visibility === 'private') {
      res.set(headers);
    }

    res.render('notes/view', {
      title: note.title,
      note,
      avgRating: avg,
      ratingCount: count,
      shareLinks,
      csrfToken: req.csrfToken ? req.csrfToken() : '',
      currentUser: req.currentUser || null
    });
  } catch (err) {
    next(err);
  }
});

// ─── Edit Note ───────────────────────────────────────────────────────────────

router.get('/:id/edit', authenticate, noCacheForPrivate, async (req, res, next) => {
  try {
    const note = await noteService.getNoteById(req.params.id, req.currentUser.id);

    if (!note || note.ownerId !== req.currentUser.id) {
      return res.status(404).render('error', {
        title: 'Not Found',
        message: 'Note not found.',
        correlationId: req.correlationId,
        currentUser: req.currentUser
      });
    }

    res.render('notes/edit', {
      title: `Edit: ${note.title}`,
      note,
      csrfToken: req.csrfToken(),
      errors: [],
      currentUser: req.currentUser
    });
  } catch (err) {
    next(err);
  }
});

router.post('/:id/edit', authenticate, [
  body('title').trim().isLength({ min: 1, max: 200 }),
  body('content').trim().isLength({ min: 1, max: 50000 }),
  body('visibility').isIn(['private', 'public'])
], async (req, res, next) => {
  const errors = validationResult(req);

  if (!errors.isEmpty()) {
    const note = await noteService.getNoteById(req.params.id, req.currentUser.id).catch(() => null);
    return res.status(422).render('notes/edit', {
      title: 'Edit Note',
      note,
      csrfToken: req.csrfToken(),
      errors: errors.array(),
      currentUser: req.currentUser
    });
  }

  try {
    const updated = await noteService.updateNote(
      req.params.id,
      req.currentUser.id,
      req.body,
      req.correlationId,
      req.ip
    );

    if (!updated) {
      return res.status(404).render('error', {
        title: 'Not Found',
        message: 'Note not found or access denied.',
        correlationId: req.correlationId,
        currentUser: req.currentUser
      });
    }

    req.flash('success', 'Note updated.');
    res.redirect(`/notes/${req.params.id}`);
  } catch (err) {
    next(err);
  }
});

// ─── Delete Note ──────────────────────────────────────────────────────────────

router.post('/:id/delete', authenticate, async (req, res, next) => {
  try {
    const deleted = await noteService.deleteNote(
      req.params.id,
      req.currentUser.id,
      req.currentUser.role,
      req.correlationId,
      req.ip
    );

    if (!deleted) {
      return res.status(404).render('error', {
        title: 'Not Found',
        message: 'Note not found or access denied.',
        correlationId: req.correlationId,
        currentUser: req.currentUser
      });
    }

    req.flash('success', 'Note deleted.');
    res.redirect('/notes');
  } catch (err) {
    next(err);
  }
});

// ─── Rate Note ───────────────────────────────────────────────────────────────

router.post('/:id/rate', authenticate, [
  body('value').isInt({ min: 1, max: 5 }),
  body('comment').optional().trim().isLength({ max: 1000 })
], async (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', 'Invalid rating value.');
    return res.redirect(`/notes/${req.params.id}`);
  }

  try {
    const { error } = await ratingService.createRating(
      req.params.id,
      req.currentUser.id,
      req.body.value,
      req.body.comment,
      req.correlationId,
      req.ip
    );

    if (error) {
      req.flash('error', error);
    } else {
      req.flash('success', 'Rating submitted.');
    }

    res.redirect(`/notes/${req.params.id}`);
  } catch (err) {
    next(err);
  }
});

// ─── View Ratings (owner/admin only) ─────────────────────────────────────────

router.get('/:id/ratings', authenticate, async (req, res, next) => {
  try {
    const note = await noteService.getNoteById(req.params.id, req.currentUser.id);

    const isOwner = note && note.ownerId === req.currentUser.id;
    const isAdmin = req.currentUser.role === constants.ROLES.ADMIN;

    if (!note || (!isOwner && !isAdmin)) {
      return res.status(403).render('error', {
        title: 'Access Denied',
        message: 'Only the note owner or admin can view ratings.',
        correlationId: req.correlationId,
        currentUser: req.currentUser
      });
    }

    const ratings = await ratingService.getRatingsForNote(note.id, note.ownerId);

    res.render('notes/view', {
      title: `Ratings: ${note.title}`,
      note,
      ratings: ratings || [],
      showRatings: true,
      avgRating: 0,
      ratingCount: 0,
      shareLinks: [],
      csrfToken: req.csrfToken(),
      currentUser: req.currentUser
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
