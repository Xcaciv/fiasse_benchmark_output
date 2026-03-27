'use strict';
const { searchLimiter } = require('../middleware/rateLimit');
const controller = require('../controllers/searchController');

module.exports = function(app, db) {
  app.get('/search', searchLimiter, (req, res) => controller.getSearch(req, res, db));
};
