'use strict';
const express = require('express');
const authRoutes = require('./auth');
const noteRoutes = require('./notes');
const attachmentRoutes = require('./attachments');
const ratingRoutes = require('./ratings');
const shareRoutes = require('./share');
const searchRoutes = require('./search');
const profileRoutes = require('./profile');
const adminRoutes = require('./admin');

function mountRoutes(app) {
  app.use('/auth', authRoutes);
  app.use('/notes', noteRoutes);
  app.use('/notes/:noteId/attachments', attachmentRoutes);
  app.use('/notes/:noteId/ratings', ratingRoutes);
  app.use('/attachments', createAttachmentDownloadRouter());
  app.use('/', shareRoutes);
  app.use('/search', searchRoutes);
  app.use('/profile', profileRoutes);
  app.use('/admin', adminRoutes);
  app.get('/', (req, res) => res.render('index'));
}

function createAttachmentDownloadRouter() {
  const router = express.Router();
  const attachmentController = require('../controllers/attachmentController');
  const { requireAuthenticated } = require('../middleware/requireAuth');
  router.get('/:id/download', requireAuthenticated, attachmentController.download);
  router.post('/:id/delete', requireAuthenticated, attachmentController.deleteAttachment);
  return router;
}

module.exports = { mountRoutes };
