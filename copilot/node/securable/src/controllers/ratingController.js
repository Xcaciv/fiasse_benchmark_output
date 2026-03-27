'use strict';

const { validationResult } = require('express-validator');
const { Rating, Note } = require('../models');
const { logActivity } = require('../services/auditService');

async function upsertRating(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', errors.array().map((e) => e.msg).join(' '));
    return res.redirect(`/notes/${req.params.id}`);
  }
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) return res.status(404).render('error', { statusCode: 404, message: 'Note not found.' });
    if (note.userId === req.user.id) {
      req.flash('error', 'You cannot rate your own note.');
      return res.redirect(`/notes/${note.id}`);
    }
    const { value, comment } = req.body;
    const [rating, created] = await Rating.findOrCreate({
      where: { noteId: note.id, userId: req.user.id },
      defaults: { value, comment: comment || null },
    });
    if (!created) {
      await rating.update({ value, comment: comment || null });
    }
    await logActivity({ userId: req.user.id, action: created ? 'rating.create' : 'rating.update', targetType: 'Note', targetId: note.id, ipAddress: req.ip });
    req.flash('success', 'Rating saved.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
}

async function getRatings(req, res, next) {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) return res.status(404).json({ error: 'Note not found.' });
    const isOwner = req.user && req.user.id === note.userId;
    if (isOwner) {
      const ratings = await Rating.findAll({ where: { noteId: note.id }, order: [['createdAt', 'DESC']] });
      return res.json({ ratings });
    }
    const ratings = await Rating.findAll({ where: { noteId: note.id } });
    const avg = ratings.length ? (ratings.reduce((s, r) => s + r.value, 0) / ratings.length).toFixed(1) : null;
    return res.json({ count: ratings.length, average: avg });
  } catch (err) {
    return next(err);
  }
}

module.exports = { upsertRating, getRatings };
