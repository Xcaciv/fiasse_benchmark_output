'use strict';
const { requireAdmin, requireReAuth } = require('../middleware/auth');
const controller = require('../controllers/adminController');

module.exports = function(app, db) {
  app.get('/admin', requireAdmin, (req, res) => controller.getDashboard(req, res, db));
  app.get('/admin/users', requireAdmin, (req, res) => controller.getUsers(req, res, db));
  app.get('/admin/reauth', requireAdmin, (req, res) => controller.getReauth(req, res));
  app.post('/admin/reauth', requireAdmin, (req, res) => controller.postReauth(req, res, db));
  app.post('/admin/users/:id/role', requireAdmin, requireReAuth, (req, res) => controller.postChangeRole(req, res, db));
  app.post('/admin/notes/:id/reassign', requireAdmin, requireReAuth, (req, res) => controller.postReassignNote(req, res, db));
};
