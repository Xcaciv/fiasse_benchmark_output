'use strict';
const { validationResult } = require('express-validator');
const { User, Note } = require('../models');
const noteService = require('../services/noteService');
const { createAuditService } = require('../services/auditService');
const logger = require('../utils/logger');
const { Op } = require('sequelize');
const { canonicalize } = require('../utils/inputHandler');

const audit = createAuditService(logger);

async function dashboard(req, res, next) {
  const totalUsers = await User.count();
  const totalNotes = await Note.count();
  const recentUsers = await User.findAll({ order: [['createdAt', 'DESC']], limit: 5, attributes: ['id', 'username', 'email', 'createdAt'] });
  return res.render('admin/dashboard', { totalUsers, totalNotes, recentUsers });
}

async function listUsers(req, res, next) {
  const users = await User.findAll({
    attributes: ['id', 'username', 'email', 'isAdmin', 'createdAt'],
    include: [{ model: Note, attributes: ['id'] }],
    order: [['createdAt', 'DESC']]
  });
  return res.render('admin/users', { users, query: '' });
}

async function searchUsers(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) return res.render('admin/users', { users: [], query: '' });
  const q = canonicalize(req.query.q || '');
  const users = await User.findAll({
    where: {
      [Op.or]: [
        { username: { [Op.like]: `%${q}%` } },
        { email: { [Op.like]: `%${q}%` } }
      ]
    },
    attributes: ['id', 'username', 'email', 'isAdmin', 'createdAt'],
    include: [{ model: Note, attributes: ['id'] }]
  });
  return res.render('admin/users', { users, query: q });
}

async function reassignNote(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', 'Invalid input.');
    return res.redirect('/admin/users');
  }
  const { noteId } = req.params;
  const { newOwnerId } = req.body;
  const result = await noteService.reassignNote(noteId, newOwnerId, req.user.id, { Note, User });
  if (!result.success) {
    req.flash('error', result.error);
    return res.redirect('/admin/users');
  }
  audit.logAdminAction('REASSIGN_NOTE', req.user.id, noteId, { newOwnerId });
  req.flash('success', 'Note reassigned.');
  return res.redirect('/admin/users');
}

module.exports = { dashboard, listUsers, searchUsers, reassignNote };
