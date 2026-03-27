'use strict';
const { requireAuth } = require('../middleware/auth');
const controller = require('../controllers/ratingController');

module.exports = function(app, db) {
  app.post('/notes/:id/ratings', requireAuth, (req, res) => controller.postCreateRating(req, res, db));
  app.get('/notes/:id/ratings', requireAuth, (req, res) => controller.getRatings(req, res, db));
  app.post('/notes/:id/ratings/:ratingId/edit', requireAuth, (req, res) => controller.postUpdateRating(req, res, db));
};
