'use strict';

const path = require('path');
const fs = require('fs');
const { validationResult } = require('express-validator');
const { Note, Attachment, Rating, ShareLink, User } = require('../models');
const { logActivity } = require('../services/auditService');
const { generateToken } = require('../utils/tokenUtils');
const { UPLOAD_DIR } = require('../config/upload');
const { sequelize } = require('../models');
const { Op, fn, col, literal } = require('sequelize');

const PAGE_SIZE = 15;

async function getMyNotes(req, res, next) {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const offset = (page - 1) * PAGE_SIZE;
    const { count, rows: notes } = await Note.findAndCountAll({
      where: { userId: req.user.id },
      order: [['updatedAt', 'DESC']],
      limit: PAGE_SIZE,
      offset,
    });
    res.render('notes/index', {
      title: 'My Notes',
      notes,
      page,
      totalPages: Math.ceil(count / PAGE_SIZE),
      csrfToken: req.csrfToken(),
    });
  } catch (err) {
    next(err);
  }
}

async function getCreateNotePage(req, res) {
  res.render('notes/create', { title: 'New Note', csrfToken: req.csrfToken() });
}

async function postCreateNote(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', errors.array().map((e) => e.msg).join(' '));
    return res.redirect('/notes/new');
  }
  try {
    const { title, content, visibility } = req.body;
    const note = await Note.create({
      title,
      content,
      visibility: ['public', 'private'].includes(visibility) ? visibility : 'private',
      userId: req.user.id, // Derived Integrity: never from body
    });
    await logActivity({ userId: req.user.id, action: 'note.create', targetType: 'Note', targetId: note.id, ipAddress: req.ip });
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
}

async function getNoteView(req, res, next) {
  try {
    const note = await Note.findByPk(req.params.id, {
      include: [
        { model: User, as: 'author', attributes: ['username'] },
        { model: Attachment },
        { model: Rating, include: [{ model: User, as: 'rater', attributes: ['username'] }], order: [['createdAt', 'DESC']] },
      ],
    });
    if (!note) return res.status(404).render('error', { statusCode: 404, message: 'Note not found.' });
    const isOwner = req.user && req.user.id === note.userId;
    if (note.visibility === 'private' && !isOwner && (!req.user || req.user.role !== 'admin')) {
      return res.status(403).render('error', { statusCode: 403, message: 'Access denied.' });
    }
    const avgRating = note.Ratings.length
      ? (note.Ratings.reduce((s, r) => s + r.value, 0) / note.Ratings.length).toFixed(1)
      : null;
    const shareLinks = isOwner ? await ShareLink.findAll({ where: { noteId: note.id, revokedAt: null } }) : [];
    res.render('notes/view', {
      title: note.title,
      note,
      avgRating,
      shareLinks,
      isOwner,
      csrfToken: req.csrfToken(),
    });
  } catch (err) {
    next(err);
  }
}

async function getEditNotePage(req, res, next) {
  try {
    const note = req.note || await Note.findByPk(req.params.id, { include: [Attachment] });
    if (!note) return res.status(404).render('error', { statusCode: 404, message: 'Note not found.' });
    res.render('notes/edit', { title: 'Edit Note', note, csrfToken: req.csrfToken() });
  } catch (err) {
    next(err);
  }
}

async function putUpdateNote(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', errors.array().map((e) => e.msg).join(' '));
    return res.redirect(`/notes/${req.params.id}/edit`);
  }
  try {
    const note = req.note;
    const { title, content, visibility } = req.body;
    await note.update({
      title,
      content,
      visibility: ['public', 'private'].includes(visibility) ? visibility : note.visibility,
    });
    await logActivity({ userId: req.user.id, action: 'note.update', targetType: 'Note', targetId: note.id, ipAddress: req.ip });
    req.flash('success', 'Note updated.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
}

async function deleteNote(req, res, next) {
  try {
    const note = req.note;
    const attachments = await Attachment.findAll({ where: { noteId: note.id } });
    const uploadDir = path.resolve(UPLOAD_DIR);
    for (const att of attachments) {
      const filePath = path.resolve(uploadDir, att.storedName);
      if (filePath.startsWith(uploadDir) && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    }
    await note.destroy();
    await logActivity({ userId: req.user.id, action: 'note.delete', targetType: 'Note', targetId: note.id, ipAddress: req.ip });
    req.flash('success', 'Note deleted.');
    return res.redirect('/notes');
  } catch (err) {
    return next(err);
  }
}

async function postGenerateShareLink(req, res, next) {
  try {
    const note = req.note;
    const token = generateToken();
    await ShareLink.create({ noteId: note.id, token });
    await logActivity({ userId: req.user.id, action: 'note.share.create', targetType: 'Note', targetId: note.id, ipAddress: req.ip });
    req.flash('success', 'Share link created.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
}

async function deleteRevokeShareLinks(req, res, next) {
  try {
    const note = req.note;
    await ShareLink.update({ revokedAt: new Date() }, { where: { noteId: note.id, revokedAt: null } });
    await logActivity({ userId: req.user.id, action: 'note.share.revoke', targetType: 'Note', targetId: note.id, ipAddress: req.ip });
    req.flash('success', 'Share links revoked.');
    return res.redirect(`/notes/${note.id}`);
  } catch (err) {
    return next(err);
  }
}

module.exports = {
  getMyNotes,
  getCreateNotePage,
  postCreateNote,
  getNoteView,
  getEditNotePage,
  putUpdateNote,
  deleteNote,
  postGenerateShareLink,
  deleteRevokeShareLinks,
};
