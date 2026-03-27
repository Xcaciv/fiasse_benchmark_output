'use strict';
const { v4: uuidv4 } = require('uuid');
const noteModel = require('../models/noteModel');
const shareLinkModel = require('../models/shareLinkModel');
const attachmentModel = require('../models/attachmentModel');
const tokenService = require('../services/tokenService');
const auditService = require('../services/auditService');

function postCreateShare(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  if (!note || note.user_id !== req.session.userId) {
    return res.status(404).render('errors/404', {});
  }

  // Revoke any existing share link first
  shareLinkModel.deleteByNoteId(db, note.id);

  const token = tokenService.generateSecureToken();
  const tokenHash = tokenService.hashToken(token);
  shareLinkModel.createShareLink(db, { id: uuidv4(), noteId: note.id, tokenHash });

  auditService.log({ eventType: 'SHARE_LINK_CREATED', userId: req.session.userId, resourceType: 'note', resourceId: note.id });
  req.session.flash = { success: `Share link: /s/${token}` };
  res.redirect(`/notes/${note.id}`);
}

function postRevokeShare(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  if (!note || note.user_id !== req.session.userId) {
    return res.status(404).render('errors/404', {});
  }

  shareLinkModel.deleteByNoteId(db, note.id);
  auditService.log({ eventType: 'SHARE_LINK_REVOKED', userId: req.session.userId, resourceType: 'note', resourceId: note.id });
  req.session.flash = { success: 'Share link revoked.' };
  res.redirect(`/notes/${note.id}`);
}

function getSharedNote(req, res, db) {
  const tokenHash = tokenService.hashToken(req.params.token);
  const shareLink = shareLinkModel.findByTokenHash(db, tokenHash);

  if (!shareLink) return res.status(404).render('errors/404', {});

  const note = noteModel.findById(db, shareLink.note_id);
  if (!note) return res.status(404).render('errors/404', {});

  const attachments = attachmentModel.findByNoteId(db, note.id);
  res.render('notes/shared', { note, attachments, token: req.params.token });
}

module.exports = { postCreateShare, postRevokeShare, getSharedNote };
