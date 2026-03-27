'use strict';
const { requireAuth } = require('../middleware/auth');
const controller = require('../controllers/shareController');

module.exports = function(app, db) {
  app.post('/notes/:id/share', requireAuth, (req, res) => controller.postCreateShare(req, res, db));
  app.post('/notes/:id/share/revoke', requireAuth, (req, res) => controller.postRevokeShare(req, res, db));
  app.get('/s/:token', (req, res) => controller.getSharedNote(req, res, db));
};
