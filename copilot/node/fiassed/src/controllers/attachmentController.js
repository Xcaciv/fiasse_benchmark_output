'use strict';
const { v4: uuidv4 } = require('uuid');
const path = require('path');
const noteModel = require('../models/noteModel');
const attachmentModel = require('../models/attachmentModel');
const shareLinkModel = require('../models/shareLinkModel');
const fileService = require('../services/fileService');
const tokenService = require('../services/tokenService');
const auditService = require('../services/auditService');
const { sanitizeFilename } = require('../utils/validation');

function postUploadAttachment(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  if (!note || note.user_id !== req.session.userId) {
    return res.status(404).render('errors/404', {});
  }
  if (!req.file) {
    req.session.flash = { error: 'No file provided.' };
    return res.redirect(`/notes/${note.id}`);
  }

  try {
    const { storedFilename, mimeType, fileSize } = fileService.validateAndStoreFile(
      req.file.buffer,
      req.file.originalname
    );
    const id = uuidv4();
    attachmentModel.createAttachment(db, {
      id,
      noteId: note.id,
      userId: req.session.userId,
      originalFilename: sanitizeFilename(req.file.originalname),
      storedFilename,
      mimeType,
      fileSize,
    });
    auditService.log({ eventType: 'ATTACHMENT_UPLOADED', userId: req.session.userId, resourceType: 'attachment', resourceId: id });
    req.session.flash = { success: 'File uploaded successfully.' };
  } catch (err) {
    req.session.flash = { error: `Upload failed: ${err.message}` };
  }

  res.redirect(`/notes/${note.id}`);
}

function getDownloadAttachment(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  const attachment = attachmentModel.findById(db, req.params.attachId);

  if (!attachment || !note || attachment.note_id !== note.id) {
    return res.status(404).render('errors/404', {});
  }

  // Allow owner or users with valid share token
  const shareToken = req.query.token;
  const isOwner = req.session && req.session.userId === note.user_id;
  let authorized = isOwner;

  if (!authorized && shareToken) {
    const tokenHash = tokenService.hashToken(shareToken);
    const shareLink = shareLinkModel.findByTokenHash(db, tokenHash);
    authorized = shareLink && shareLink.note_id === note.id;
  }

  if (!authorized) {
    return res.status(404).render('errors/404', {});
  }

  const filePath = fileService.getFilePath(attachment.stored_filename);
  res.setHeader('Content-Disposition', `attachment; filename="${attachment.original_filename}"`);
  res.setHeader('Content-Type', attachment.mime_type);
  res.setHeader('X-Content-Type-Options', 'nosniff');
  auditService.log({ eventType: 'ATTACHMENT_DOWNLOADED', userId: req.session ? req.session.userId : null, resourceType: 'attachment', resourceId: attachment.id });
  res.sendFile(filePath);
}

function postDeleteAttachment(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  const attachment = attachmentModel.findById(db, req.params.attachId);

  if (!note || !attachment || note.user_id !== req.session.userId || attachment.note_id !== note.id) {
    return res.status(404).render('errors/404', {});
  }

  attachmentModel.deleteById(db, attachment.id);
  fileService.deleteFile(attachment.stored_filename);

  auditService.log({ eventType: 'ATTACHMENT_DELETED', userId: req.session.userId, resourceType: 'attachment', resourceId: attachment.id });
  req.session.flash = { success: 'Attachment deleted.' };
  res.redirect(`/notes/${note.id}`);
}

module.exports = { postUploadAttachment, getDownloadAttachment, postDeleteAttachment };
