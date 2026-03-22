'use strict';
const { validationResult } = require('express-validator');
const noteService = require('../services/noteService');
const { Note, User, sequelize } = require('../models');

async function search(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('search/results', { notes: [], query: '', errors: errors.array() });
  }
  const query = req.query.q || '';
  const userId = req.user ? req.user.id : null;
  const notes = await noteService.searchNotes(query, userId, { Note, User, sequelize });
  return res.render('search/results', { notes, query, errors: [] });
}

module.exports = { search };
