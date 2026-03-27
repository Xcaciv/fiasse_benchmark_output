'use strict';
const noteModel = require('../models/noteModel');
const { validateSearchQuery } = require('../utils/validation');

function getSearch(req, res, db) {
  const query = req.query.q || '';

  if (!query.trim()) {
    return res.render('search/results', { notes: [], query: '', error: null });
  }

  const queryCheck = validateSearchQuery(query);
  if (!queryCheck.valid) {
    return res.render('search/results', { notes: [], query, error: queryCheck.reason });
  }

  const userId = req.session && req.session.userId ? req.session.userId : '';
  const notes = noteModel.searchNotes(db, userId, query.trim());
  res.render('search/results', { notes, query, error: null });
}

module.exports = { getSearch };
