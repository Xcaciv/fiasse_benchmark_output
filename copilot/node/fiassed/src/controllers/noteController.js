'use strict';
const { v4: uuidv4 } = require('uuid');
const noteModel = require('../models/noteModel');
const attachmentModel = require('../models/attachmentModel');
const shareLinkModel = require('../models/shareLinkModel');
const ratingModel = require('../models/ratingModel');
const fileService = require('../services/fileService');
const auditService = require('../services/auditService');
const { validateNoteTitle, validateNoteContent } = require('../utils/validation');

function getNotes(req, res, db) {
  const notes = noteModel.findAllByUser(db, req.session.userId);
  res.render('notes/list', { notes });
}

function getCreateNote(req, res) {
  res.render('notes/create', { error: null });
}

function postCreateNote(req, res, db) {
  const { title, content, visibility } = req.body;
  const userId = req.session.userId; // Always from session, never from body

  const titleCheck = validateNoteTitle(title);
  if (!titleCheck.valid) {
    return res.render('notes/create', { error: titleCheck.reason });
  }
  const contentCheck = validateNoteContent(content);
  if (!contentCheck.valid) {
    return res.render('notes/create', { error: contentCheck.reason });
  }

  const safeVisibility = visibility === 'public' ? 'public' : 'private';
  const id = uuidv4();
  noteModel.createNote(db, { id, userId, title, content, visibility: safeVisibility });
  auditService.log({ eventType: 'NOTE_CREATED', userId, resourceType: 'note', resourceId: id });
  res.redirect('/notes');
}

function getNote(req, res, db) {
  const note = noteModel.findByIdForUser(db, req.params.id, req.session.userId);
  if (!note) return res.status(404).render('errors/404', {});

  const attachments = attachmentModel.findByNoteId(db, note.id);
  const ratingStats = ratingModel.getAverageRating(db, note.id);
  const userRating = ratingModel.findByNoteAndUser(db, note.id, req.session.userId);
  const shareLink = note.user_id === req.session.userId ? shareLinkModel.findByNoteId(db, note.id) : null;
  const isOwner = note.user_id === req.session.userId;

  res.render('notes/view', { note, attachments, ratingStats, userRating, shareLink, isOwner });
}

function getEditNote(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  if (!note || note.user_id !== req.session.userId) {
    return res.status(404).render('errors/404', {});
  }
  res.render('notes/edit', { note, error: null });
}

function postEditNote(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  if (!note || note.user_id !== req.session.userId) {
    return res.status(404).render('errors/404', {});
  }

  const { title, content, visibility } = req.body;
  const titleCheck = validateNoteTitle(title);
  if (!titleCheck.valid) {
    return res.render('notes/edit', { note, error: titleCheck.reason });
  }
  const contentCheck = validateNoteContent(content);
  if (!contentCheck.valid) {
    return res.render('notes/edit', { note, error: contentCheck.reason });
  }

  const safeVisibility = visibility === 'public' ? 'public' : 'private';
  noteModel.updateNote(db, note.id, { title, content, visibility: safeVisibility });
  auditService.log({ eventType: 'NOTE_UPDATED', userId: req.session.userId, resourceType: 'note', resourceId: note.id });
  res.redirect(`/notes/${note.id}`);
}

function postDeleteNote(req, res, db) {
  const note = noteModel.findById(db, req.params.id);
  if (!note || note.user_id !== req.session.userId) {
    return res.status(404).render('errors/404', {});
  }

  // Get attachment filenames before cascade delete
  const storedFilenames = attachmentModel.deleteByNoteId(db, note.id);
  noteModel.deleteNote(db, note.id);

  // Delete files from disk after DB records removed
  storedFilenames.forEach(filename => fileService.deleteFile(filename));

  auditService.log({ eventType: 'NOTE_DELETED', userId: req.session.userId, resourceType: 'note', resourceId: note.id });
  req.session.flash = { success: 'Note deleted.' };
  res.redirect('/notes');
}

module.exports = { getNotes, getCreateNote, postCreateNote, getNote, getEditNote, postEditNote, postDeleteNote };
