'use strict';
const controller = require('../controllers/publicController');

module.exports = function(app, db) {
  app.get('/top-rated', (req, res) => controller.getTopRated(req, res, db));
  app.get('/', (req, res) => {
    if (req.session && req.session.userId) {
      return res.redirect('/notes');
    }
    res.redirect('/login');
  });
};
