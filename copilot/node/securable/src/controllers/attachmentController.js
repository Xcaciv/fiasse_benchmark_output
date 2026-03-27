'use strict';

const path = require('path');
const fs = require('fs');
const { Note, Attachment, ShareLink } = require('../models');
const { logActivity } = require('../services/auditService');
const { UPLOAD_DIR } = require('../config/upload');
const logger = require('../utils/logger');

async function postUploadAttachment(req, res, next) {
  try {
    if (!req.file) {
      req.flash('error', 'No file uploaded or file type not allowed.');
      return res.redirect(`/notes/${req.params.id}/edit`);
    }
    const note = req.note;
    const existingCount = await Attachment.count({ where: { noteId: note.id } });
    if (existingCount >= 5) {
      fs.unlinkSync(req.file.path);
      req.flash('error', 'Maximum of 5 attachments per note.');
      return res.redirect(`/notes/${note.id}/edit`);
    }
    const sanitizedName = path.basename(req.file.originalname);
    await Attachment.create({
      noteId: note.id,
      originalName: sanitizedName,
      storedName: req.file.filename,
      mimeType: req.file.mimetype,
      size: req.file.size,
    });
    await logActivity({ userId: req.user.id, action: 'attachment.upload', targetType: 'Note', targetId: note.id, ipAddress: req.ip });
    req.flash('success', 'File uploaded.');
    return res.redirect(`/notes/${note.id}/edit`);
  } catch (err) {
    return next(err);
  }
}

async function downloadAttachment(req, res, next) {
  try {
    const attachment = await Attachment.findOne({
      where: { id: req.params.attachId, noteId: req.params.id },
    });
    if (!attachment) {
      return res.status(404).render('error', { statusCode: 404, message: 'Attachment not found.' });
    }
    const note = await Note.findByPk(req.params.id);
    const isOwner = req.user && req.user.id === note.userId;
    const isAdmin = req.user && req.user.role === 'admin';

    if (!isOwner && !isAdmin) {
      const shareToken = req.query.token;
      if (!shareToken) {
        return res.status(403).render('error', { statusCode: 403, message: 'Access denied.' });
      }
      const shareLink = await ShareLink.findOne({ where: { noteId: note.id, token: shareToken, revokedAt: null } });
      if (!shareLink) {
        return res.status(403).render('error', { statusCode: 403, message: 'Access denied.' });
      }
    }

    const uploadDir = path.resolve(UPLOAD_DIR);
    const filePath = path.resolve(uploadDir, attachment.storedName);

    if (!filePath.startsWith(uploadDir)) {
      logger.error('Path traversal attempt blocked', { storedName: attachment.storedName, ip: req.ip });
      return res.status(400).render('error', { statusCode: 400, message: 'Invalid file path.' });
    }

    if (!fs.existsSync(filePath)) {
      return res.status(404).render('error', { statusCode: 404, message: 'File not found on disk.' });
    }

    res.setHeader('Content-Disposition', `attachment; filename="${attachment.originalName}"`);
    res.setHeader('Content-Type', attachment.mimeType);
    res.sendFile(filePath);
  } catch (err) {
    next(err);
  }
}

async function deleteAttachment(req, res, next) {
  try {
    const attachment = await Attachment.findOne({
      where: { id: req.params.attachId, noteId: req.params.id },
    });
    if (!attachment) {
      return res.status(404).render('error', { statusCode: 404, message: 'Attachment not found.' });
    }
    const uploadDir = path.resolve(UPLOAD_DIR);
    const filePath = path.resolve(uploadDir, attachment.storedName);
    if (filePath.startsWith(uploadDir) && fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
    await attachment.destroy();
    await logActivity({ userId: req.user.id, action: 'attachment.delete', targetType: 'Attachment', targetId: attachment.id, ipAddress: req.ip });
    req.flash('success', 'Attachment deleted.');
    return res.redirect(`/notes/${req.params.id}/edit`);
  } catch (err) {
    return next(err);
  }
}

module.exports = { postUploadAttachment, downloadAttachment, deleteAttachment };
