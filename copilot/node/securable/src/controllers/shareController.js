'use strict';
const { ShareLink, Note, User, Attachment, Rating } = require('../models');
const { generateSecureToken } = require('../utils/tokenHelper');
const noteService = require('../services/noteService');

async function generateShareLink(req, res, next) {
  const { noteId } = req.params;
  const note = await Note.findOne({ where: { id: noteId, userId: req.user.id } });
  if (!note) return res.status(403).render('error', { message: 'Access denied', layout: 'layouts/main' });
  await ShareLink.destroy({ where: { noteId } });
  const token = generateSecureToken(32);
  await ShareLink.create({ noteId, token });
  req.flash('success', 'Share link generated.');
  return res.redirect(`/notes/${noteId}`);
}

async function revokeShareLink(req, res, next) {
  const { noteId } = req.params;
  const note = await Note.findOne({ where: { id: noteId, userId: req.user.id } });
  if (!note) return res.status(403).render('error', { message: 'Access denied', layout: 'layouts/main' });
  await ShareLink.destroy({ where: { noteId } });
  req.flash('success', 'Share link revoked.');
  return res.redirect(`/notes/${noteId}`);
}

async function viewShared(req, res, next) {
  const { token } = req.params;
  const shareLink = await ShareLink.findOne({ where: { token } });
  if (!shareLink) return res.status(404).render('error', { message: 'Share link not found', layout: 'layouts/main' });
  const note = await noteService.getNoteById(shareLink.noteId, null, false, { Note, User });
  if (!note) return res.status(404).render('error', { message: 'Note not found', layout: 'layouts/main' });
  const ratings = await Rating.findAll({ where: { noteId: note.id }, include: [{ model: User, attributes: ['username'] }] });
  const attachments = await Attachment.findAll({ where: { noteId: note.id } });
  const avgRating = ratings.length ? (ratings.reduce((s, r) => s + r.stars, 0) / ratings.length).toFixed(1) : null;
  return res.render('notes/view', { note, ratings, attachments, shareLinks: [], avgRating, isSharedView: true });
}

module.exports = { generateShareLink, revokeShareLink, viewShared };
