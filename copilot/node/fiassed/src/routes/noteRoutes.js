'use strict';
const { requireAuth } = require('../middleware/auth');
const controller = require('../controllers/noteController');

module.exports = function(app, db) {
  app.get('/notes', requireAuth, (req, res) => controller.getNotes(req, res, db));
  app.get('/notes/create', requireAuth, (req, res) => controller.getCreateNote(req, res));
  app.post('/notes', requireAuth, (req, res) => controller.postCreateNote(req, res, db));
  app.get('/notes/:id', requireAuth, (req, res) => controller.getNote(req, res, db));
  app.get('/notes/:id/edit', requireAuth, (req, res) => controller.getEditNote(req, res, db));
  app.post('/notes/:id/edit', requireAuth, (req, res) => controller.postEditNote(req, res, db));
  app.post('/notes/:id/delete', requireAuth, (req, res) => controller.postDeleteNote(req, res, db));
};
