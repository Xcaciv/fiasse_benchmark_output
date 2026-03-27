'use strict';
const { loginLimiter, registrationLimiter } = require('../middleware/rateLimit');
const controller = require('../controllers/authController');

module.exports = function(app, db) {
  app.get('/register', (req, res) => controller.getRegister(req, res));
  app.post('/register', registrationLimiter, (req, res) => controller.postRegister(req, res, db));

  app.get('/login', (req, res) => controller.getLogin(req, res));
  app.post('/login', loginLimiter, (req, res) => controller.postLogin(req, res, db));

  app.post('/logout', (req, res) => controller.postLogout(req, res));

  app.get('/forgot-password', (req, res) => controller.getForgotPassword(req, res));
  app.post('/forgot-password', loginLimiter, (req, res) => controller.postForgotPassword(req, res, db));

  app.get('/reset-password/:token', (req, res) => controller.getResetPassword(req, res, db));
  app.post('/reset-password/:token', (req, res) => controller.postResetPassword(req, res, db));
};
