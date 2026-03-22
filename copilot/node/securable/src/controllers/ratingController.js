'use strict';
const { validationResult } = require('express-validator');
const { Rating, Note } = require('../models');
const { canonicalize } = require('../utils/inputHandler');

async function addOrUpdateRating(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', errors.array().map(e => e.msg).join(', '));
    return res.redirect('back');
  }
  const { noteId } = req.params;
  const { stars, comment } = req.body;
  const note = await Note.findByPk(noteId);
  if (!note || (!note.isPublic && note.userId !== req.user.id)) {
    return res.status(403).render('error', { message: 'Access denied', layout: 'layouts/main' });
  }
  const existing = await Rating.findOne({ where: { noteId, userId: req.user.id } });
  const sanitizedComment = comment ? canonicalize(comment) : null;
  if (existing) {
    await existing.update({ stars: parseInt(stars, 10), comment: sanitizedComment });
  } else {
    await Rating.create({ noteId, userId: req.user.id, stars: parseInt(stars, 10), comment: sanitizedComment });
  }
  req.flash('success', 'Rating saved.');
  return res.redirect(`/notes/${noteId}`);
}

module.exports = { addOrUpdateRating };
