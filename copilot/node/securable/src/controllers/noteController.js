'use strict';
const noteService = require('../services/noteService');
const { createAuditService } = require('../services/auditService');
const { validationResult } = require('express-validator');
const logger = require('../utils/logger');
const { Note, User, Rating, Attachment, ShareLink, sequelize } = require('../models');

const audit = createAuditService(logger);

async function listMyNotes(req, res, next) {
  const notes = await noteService.getUserNotes(req.user.id, { Note });
  return res.render('notes/index', { notes });
}

async function viewNote(req, res, next) {
  const { id } = req.params;
  const userId = req.user ? req.user.id : null;
  const isAdmin = req.user ? req.user.isAdmin : false;
  const note = await noteService.getNoteById(id, userId, isAdmin, { Note, User });
  if (!note) return res.status(404).render('error', { message: 'Note not found', layout: 'layouts/main' });
  const ratings = await Rating.findAll({ where: { noteId: id }, include: [{ model: User, attributes: ['username'] }], order: [['createdAt', 'DESC']] });
  const attachments = await Attachment.findAll({ where: { noteId: id } });
  const shareLinks = userId === note.userId ? await ShareLink.findAll({ where: { noteId: id } }) : [];
  const avgRating = ratings.length ? (ratings.reduce((s, r) => s + r.stars, 0) / ratings.length).toFixed(1) : null;
  return res.render('notes/view', { note, ratings, attachments, shareLinks, avgRating, isSharedView: false });
}

async function createNote(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.render('notes/create', { errors: errors.array(), csrfToken: req.csrfToken() });
  }
  const { title, content, isPublic } = req.body;
  const note = await noteService.createNote({ title, content, userId: req.user.id }, { Note });
  if (isPublic === 'on' || isPublic === 'true') await note.update({ isPublic: true });
  audit.logNoteAction('CREATE', req.user.id, note.id, {});
  return res.redirect(`/notes/${note.id}`);
}

async function editNoteForm(req, res, next) {
  const note = await noteService.getNoteById(req.params.id, req.user.id, req.user.isAdmin, { Note, User });
  if (!note) return res.status(404).render('error', { message: 'Note not found', layout: 'layouts/main' });
  const attachments = await Attachment.findAll({ where: { noteId: note.id } });
  return res.render('notes/edit', { note, attachments, errors: [], csrfToken: req.csrfToken() });
}

async function updateNote(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const note = await Note.findByPk(req.params.id);
    return res.render('notes/edit', { note, attachments: [], errors: errors.array(), csrfToken: req.csrfToken() });
  }
  const { title, content, isPublic } = req.body;
  const result = await noteService.updateNote(req.params.id, req.user.id, { title, content, isPublic: isPublic === 'on' }, { Note });
  if (!result.success) return res.status(403).render('error', { message: result.error, layout: 'layouts/main' });
  audit.logNoteAction('UPDATE', req.user.id, req.params.id, {});
  return res.redirect(`/notes/${req.params.id}`);
}

async function deleteNote(req, res, next) {
  const result = await noteService.deleteNote(req.params.id, req.user.id, req.user.isAdmin, { Note, Attachment, Rating, ShareLink });
  if (!result.success) return res.status(403).render('error', { message: result.error, layout: 'layouts/main' });
  audit.logNoteAction('DELETE', req.user.id, req.params.id, {});
  req.flash('success', 'Note deleted.');
  return res.redirect('/notes');
}

async function topRated(req, res, next) {
  const notes = await noteService.getTopRatedNotes({ Note, Rating, User, sequelize });
  return res.render('notes/top-rated', { notes });
}

module.exports = { listMyNotes, viewNote, createNote, editNoteForm, updateNote, deleteNote, topRated };
