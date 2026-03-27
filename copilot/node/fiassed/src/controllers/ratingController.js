'use strict';
const { v4: uuidv4 } = require('uuid');
const noteModel = require('../models/noteModel');
const ratingModel = require('../models/ratingModel');
const auditService = require('../services/auditService');
const { validateRating } = require('../utils/validation');
const config = require('../config');

function postCreateRating(req, res, db) {
  const note = noteModel.findByIdForUser(db, req.params.id, req.session.userId);
  if (!note) return res.status(404).render('errors/404', {});

  // Prevent self-rating
  if (note.user_id === req.session.userId) {
    req.session.flash = { error: 'You cannot rate your own note.' };
    return res.redirect(`/notes/${note.id}`);
  }

  const { rating, comment } = req.body;
  const ratingCheck = validateRating(rating);
  if (!ratingCheck.valid) {
    req.session.flash = { error: ratingCheck.reason };
    return res.redirect(`/notes/${note.id}`);
  }

  const safeComment = comment && typeof comment === 'string'
    ? comment.slice(0, config.ratingCommentMaxLength)
    : null;

  const existing = ratingModel.findByNoteAndUser(db, note.id, req.session.userId);
  if (existing) {
    ratingModel.updateRating(db, existing.id, { rating: parseInt(rating, 10), comment: safeComment });
    auditService.log({ eventType: 'RATING_UPDATED', userId: req.session.userId, resourceType: 'rating', resourceId: existing.id });
  } else {
    const id = uuidv4();
    ratingModel.createRating(db, { id, noteId: note.id, userId: req.session.userId, rating: parseInt(rating, 10), comment: safeComment });
    auditService.log({ eventType: 'RATING_CREATED', userId: req.session.userId, resourceType: 'rating', resourceId: id });
  }

  req.session.flash = { success: 'Rating submitted.' };
  res.redirect(`/notes/${note.id}`);
}

function getRatings(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  if (!note || note.user_id !== req.session.userId) {
    return res.status(404).render('errors/404', {});
  }
  const ratings = ratingModel.findByNoteId(db, note.id);
  res.render('ratings/manage', { note, ratings });
}

function postUpdateRating(req, res, db) {
  const ratingRecord = ratingModel.findByNoteAndUser(db, req.params.id, req.session.userId);
  if (!ratingRecord || ratingRecord.id !== req.params.ratingId) {
    return res.status(404).render('errors/404', {});
  }

  const { rating, comment } = req.body;
  const ratingCheck = validateRating(rating);
  if (!ratingCheck.valid) {
    req.session.flash = { error: ratingCheck.reason };
    return res.redirect(`/notes/${req.params.id}`);
  }

  const safeComment = comment && typeof comment === 'string'
    ? comment.slice(0, config.ratingCommentMaxLength)
    : null;

  ratingModel.updateRating(db, ratingRecord.id, { rating: parseInt(rating, 10), comment: safeComment });
  auditService.log({ eventType: 'RATING_UPDATED', userId: req.session.userId, resourceType: 'rating', resourceId: ratingRecord.id });
  req.session.flash = { success: 'Rating updated.' };
  res.redirect(`/notes/${req.params.id}`);
}

module.exports = { postCreateRating, getRatings, postUpdateRating };
