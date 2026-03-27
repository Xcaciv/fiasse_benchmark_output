'use strict';

const express = require('express');
const multer = require('multer');
const path = require('path');
const { authenticate } = require('../middleware/authenticate');
const { noCacheForPrivate } = require('../middleware/security');
const noteService = require('../services/noteService');
const fileService = require('../services/fileService');
const constants = require('../config/constants');
const { logger } = require('../config/logger');

const router = express.Router();

// Multer configured to use memory storage - file validated before disk write
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: constants.FILES.MAX_SIZE_BYTES,
    files: 1
  }
});

// ─── Upload Attachment ────────────────────────────────────────────────────────

router.post('/:noteId', authenticate, upload.single('file'), async (req, res, next) => {
  try {
    if (!req.file) {
      req.flash('error', 'No file provided.');
      return res.redirect('back');
    }

    // Integrity: verify ownership before accepting file
    const note = await noteService.getNoteById(req.params.noteId, req.currentUser.id);
    if (!note || note.ownerId !== req.currentUser.id) {
      req.flash('error', 'Note not found or access denied.');
      return res.redirect('/notes');
    }

    // Trust boundary: validate file type and size before storage
    const { valid, message, detectedMime } = await fileService.validateFile(req.file);
    if (!valid) {
      req.flash('error', message);
      return res.redirect(`/notes/${req.params.noteId}`);
    }

    await fileService.storeFile(
      req.file,
      req.params.noteId,
      detectedMime,
      req.currentUser.id,
      req.correlationId
    );

    req.flash('success', 'File uploaded.');
    res.redirect(`/notes/${req.params.noteId}`);
  } catch (err) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      req.flash('error', `File too large. Maximum size is ${process.env.MAX_FILE_SIZE_MB || 10}MB.`);
      return res.redirect('back');
    }
    next(err);
  }
});

// ─── Download Attachment ──────────────────────────────────────────────────────

router.get('/:fileId', authenticate, noCacheForPrivate, async (req, res, next) => {
  try {
    const attachment = await fileService.getFile(req.params.fileId, req.currentUser.id);

    if (!attachment) {
      return res.status(404).render('error', {
        title: 'Not Found',
        message: 'Attachment not found or access denied.',
        correlationId: req.correlationId,
        currentUser: req.currentUser
      });
    }

    const filePath = fileService.buildFilePath(attachment.storedFilename);

    // Confidentiality: force download, do not allow inline execution
    res.setHeader('Content-Disposition',
      `attachment; filename="${encodeURIComponent(attachment.originalFilename)}"`
    );
    res.setHeader('Content-Type', attachment.mimeType);
    res.setHeader('X-Content-Type-Options', 'nosniff');

    res.sendFile(filePath, { dotfiles: 'deny' }, (err) => {
      if (err) {
        logger.error('File send error', {
          event: 'file.send_error',
          fileId: req.params.fileId,
          error: err.message,
          correlationId: req.correlationId
        });
        if (!res.headersSent) {
          next(err);
        }
      }
    });
  } catch (err) {
    next(err);
  }
});

// ─── Delete Attachment ────────────────────────────────────────────────────────

router.delete('/:fileId', authenticate, async (req, res, next) => {
  try {
    const deleted = await fileService.deleteFile(
      req.params.fileId,
      req.currentUser.id,
      req.correlationId
    );

    if (!deleted) {
      req.flash('error', 'Attachment not found or access denied.');
    } else {
      req.flash('success', 'Attachment deleted.');
    }

    const referer = req.get('Referer') || '/notes';
    res.redirect(referer);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
