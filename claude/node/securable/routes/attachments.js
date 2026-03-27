'use strict';

const express = require('express');
const multer = require('multer');
const path = require('path');
const { Note, Attachment } = require('../models');
const { requireAuth } = require('../middleware/auth');
const { uploadLimiter } = require('../middleware/rateLimiter');
const audit = require('../services/auditService');
const fileStorage = require('../services/fileStorageService');
const security = require('../config/security');
const logger = require('../config/logger');

// mergeParams: true — inherit :noteId from parent router mount
const router = express.Router({ mergeParams: true });

// [TRUST BOUNDARY] Multer configuration — validates file type and size before disk write
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, fileStorage.uploadDir),
  filename: (req, file, cb) => {
    const storedName = fileStorage.buildStoredFilename(file.originalname);
    cb(null, storedName);
  },
});

function fileFilter(req, file, cb) {
  const result = fileStorage.validateFileType(file.mimetype, file.originalname);
  if (!result.valid) {
    return cb(new Error(result.reason));
  }
  return cb(null, true);
}

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: security.MAX_FILE_SIZE_BYTES },
});

// ── Upload Attachment ─────────────────────────────────────────────────────

router.post('/', requireAuth, uploadLimiter,
  (req, res, next) => {
    upload.single('file')(req, res, (err) => {
      if (err instanceof multer.MulterError || err) {
        req.flash('error', err.message || 'File upload failed.');
        return res.redirect(`/notes/${req.params.noteId}`);
      }
      return next();
    });
  },
  async (req, res, next) => {
    try {
      if (!req.file) {
        req.flash('error', 'No file selected.');
        return res.redirect(`/notes/${req.params.noteId}`);
      }

      const note = await findOwnedNote(req.params.noteId, req.user);
      if (!note) {
        // Remove the uploaded file since we cannot attach it
        await fileStorage.deleteFile(req.file.filename);
        return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });
      }

      await Attachment.create({
        noteId: note.id,
        storedFilename: req.file.filename,
        originalFilename: path.basename(req.file.originalname), // basename strips any path component
        mimeType: req.file.mimetype,
        fileSizeBytes: req.file.size,
      });

      await audit.record('attachment.upload', req.user.id, { noteId: note.id }, req.ip);
      req.flash('success', 'File attached.');
      return res.redirect(`/notes/${note.id}`);
    } catch (err) {
      if (req.file) await fileStorage.deleteFile(req.file.filename);
      return next(err);
    }
  }
);

// ── Download Attachment ───────────────────────────────────────────────────

router.get('/:attachmentId', requireAuth, async (req, res, next) => {
  try {
    const attachment = await Attachment.findByPk(req.params.attachmentId, {
      include: [{ model: Note, as: 'note' }],
    });

    if (!attachment) return res.status(404).render('error', { title: 'Not Found', message: 'Attachment not found.', statusCode: 404 });

    const note = attachment.note;
    const isOwner = note.userId === req.user.id;

    if (!note.isPublic && !isOwner && req.user.role !== 'admin') {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });
    }

    const filePath = fileStorage.resolveStoragePath(attachment.storedFilename);
    // Use originalFilename only as display name; filePath is resolved from storedFilename
    return res.download(filePath, attachment.originalFilename);
  } catch (err) {
    return next(err);
  }
});

// ── Delete Attachment ─────────────────────────────────────────────────────

router.post('/:attachmentId/delete', requireAuth, async (req, res, next) => {
  try {
    const attachment = await Attachment.findByPk(req.params.attachmentId, {
      include: [{ model: Note, as: 'note' }],
    });

    if (!attachment) return res.status(404).render('error', { title: 'Not Found', message: 'Attachment not found.', statusCode: 404 });

    const note = attachment.note;
    if (note.userId !== req.user.id && req.user.role !== 'admin') {
      return res.status(403).render('error', { title: 'Forbidden', message: 'Access denied.', statusCode: 403 });
    }

    await fileStorage.deleteFile(attachment.storedFilename);
    await attachment.destroy();
    await audit.record('attachment.delete', req.user.id, { noteId: note.id }, req.ip);
    req.flash('success', 'Attachment deleted.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
});

async function findOwnedNote(noteId, user) {
  const note = await Note.findByPk(noteId);
  if (!note) return null;
  if (note.userId !== user.id && user.role !== 'admin') return null;
  return note;
}

module.exports = router;
