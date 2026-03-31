'use strict';

const express = require('express');
const path = require('path');
const { param } = require('express-validator');
const { Attachment } = require('../models');
const noteService = require('../services/noteService');
const fileService = require('../services/fileService');
const auditService = require('../services/auditService');
const { handleValidationErrors } = require('../middleware/validate');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.use(requireAuth);

// POST /attachments/upload/:noteId
router.post(
  '/upload/:noteId',
  [param('noteId').isUUID().withMessage('Invalid note ID.')],
  handleValidationErrors,
  fileService.upload.single('file'),
  async (req, res, next) => {
    try {
      // Verify the requester owns the note
      await noteService.assertOwnership(req.params.noteId, req.user.id);

      if (!req.file) {
        req.flash('error', 'No file uploaded.');
        return res.redirect(`/notes/${req.params.noteId}`);
      }

      await Attachment.create({
        noteId: req.params.noteId,
        storedFilename: req.file.filename,
        originalFilename: path.basename(req.file.originalname),
        mimeType: req.file.mimetype,
        sizeBytes: req.file.size,
      });

      await auditService.record({
        userId: req.user.id,
        action: 'ATTACHMENT_UPLOAD',
        targetType: 'Note',
        targetId: req.params.noteId,
        metadata: { originalname: req.file.originalname, size: req.file.size },
      });

      req.flash('success', 'File uploaded successfully.');
      res.redirect(`/notes/${req.params.noteId}`);
    } catch (err) {
      if (err.status === 403) {
        req.flash('error', 'Access denied.');
        return res.redirect('/notes');
      }
      // multer errors (file type, size)
      if (err.message && (err.message.includes('File type') || err.message.includes('limit'))) {
        req.flash('error', err.message);
        return res.redirect(`/notes/${req.params.noteId}`);
      }
      next(err);
    }
  }
);

// GET /attachments/:id/download
router.get(
  '/:id/download',
  [param('id').isUUID().withMessage('Invalid attachment ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const attachment = await Attachment.findByPk(req.params.id, {
        include: [{ association: 'note', attributes: ['id', 'userId', 'isPublic'] }],
      });

      if (!attachment) return res.status(404).render('error', { title: 'Not Found', status: 404, message: 'Attachment not found.' });

      const note = attachment.note;
      if (!note.isPublic && note.userId !== req.user.id) {
        return res.status(403).render('error', { title: 'Access Denied', status: 403, message: 'Access denied.' });
      }

      const filePath = fileService.getFilePath(attachment.storedFilename);
      res.download(filePath, attachment.originalFilename);
    } catch (err) {
      next(err);
    }
  }
);

// POST /attachments/:id/delete
router.post(
  '/:id/delete',
  [param('id').isUUID().withMessage('Invalid attachment ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const attachment = await Attachment.findByPk(req.params.id, {
        include: [{ association: 'note', attributes: ['id', 'userId'] }],
      });

      if (!attachment) {
        req.flash('error', 'Attachment not found.');
        return res.redirect('/notes');
      }

      if (attachment.note.userId !== req.user.id && req.user.role !== 'admin') {
        req.flash('error', 'Access denied.');
        return res.redirect(`/notes/${attachment.noteId}`);
      }

      const noteId = attachment.noteId;
      await fileService.deleteFile(attachment.storedFilename);
      await attachment.destroy();

      await auditService.record({
        userId: req.user.id,
        action: 'ATTACHMENT_DELETE',
        targetType: 'Attachment',
        targetId: req.params.id,
      });

      req.flash('success', 'Attachment deleted.');
      res.redirect(`/notes/${noteId}`);
    } catch (err) {
      next(err);
    }
  }
);

module.exports = router;
