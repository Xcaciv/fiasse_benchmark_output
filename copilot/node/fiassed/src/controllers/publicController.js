'use strict';
const noteModel = require('../models/noteModel');

function getTopRated(req, res, db) {
  res.setHeader('Cache-Control', 'no-store');
  const notes = noteModel.getPublicNotesWithMinRatings(db, 3);
  res.render('public/top-rated', { notes });
}

module.exports = { getTopRated };
