'use strict';
const { requireAuth } = require('../middleware/auth');
const controller = require('../controllers/attachmentController');
const { upload } = require('../services/fileService');

module.exports = function(app, db) {
  app.post('/notes/:id/attachments', requireAuth, upload.single('file'), (req, res) => controller.postUploadAttachment(req, res, db));
  app.get('/notes/:id/attachments/:attachId/download', requireAuth, (req, res) => controller.getDownloadAttachment(req, res, db));
  app.post('/notes/:id/attachments/:attachId/delete', requireAuth, (req, res) => controller.postDeleteAttachment(req, res, db));
};
