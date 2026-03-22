'use strict';
const path = require('path');
const attachmentService = require('../services/attachmentService');
const { Attachment, Note } = require('../models');

async function upload(req, res, next) {
  if (!req.file) {
    req.flash('error', 'No file uploaded.');
    return res.redirect('back');
  }
  const { noteId } = req.params;
  const result = await attachmentService.saveAttachment(req.file, noteId, { Attachment });
  if (!result.success) {
    req.flash('error', result.error);
    return res.redirect('back');
  }
  req.flash('success', 'File uploaded.');
  return res.redirect(`/notes/${noteId}/edit`);
}

async function download(req, res, next) {
  const userId = req.user ? req.user.id : null;
  const isAdmin = req.user ? req.user.isAdmin : false;
  const attachment = await attachmentService.getAttachment(req.params.id, userId, isAdmin, { Attachment, Note });
  if (!attachment) return res.status(404).render('error', { message: 'Attachment not found', layout: 'layouts/main' });
  const uploadDir = process.env.UPLOAD_DIR || './uploads';
  const filePath = path.resolve(uploadDir, attachment.storedFilename);
  return res.download(filePath, attachment.originalFilename);
}

async function deleteAttachment(req, res, next) {
  const { id } = req.params;
  const attachment = await Attachment.findByPk(id);
  if (!attachment) return res.status(404).render('error', { message: 'Not found', layout: 'layouts/main' });
  const noteId = attachment.noteId;
  await attachmentService.deleteAttachment(id, noteId, req.user.id, { Attachment, Note });
  req.flash('success', 'Attachment deleted.');
  return res.redirect(`/notes/${noteId}/edit`);
}

module.exports = { upload, download, deleteAttachment };
