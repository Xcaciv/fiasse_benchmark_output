'use strict';
const { requireAuth } = require('../middleware/auth');
const controller = require('../controllers/profileController');

module.exports = function(app, db) {
  app.get('/profile', requireAuth, (req, res) => controller.getProfile(req, res, db));
  app.post('/profile', requireAuth, (req, res) => controller.postProfile(req, res, db));
};
