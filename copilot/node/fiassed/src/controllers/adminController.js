'use strict';
const userModel = require('../models/userModel');
const noteModel = require('../models/noteModel');
const passwordService = require('../services/passwordService');
const auditService = require('../services/auditService');

function getDashboard(req, res, db) {
  res.setHeader('Cache-Control', 'no-store');
  const userCount = db.prepare('SELECT COUNT(*) AS count FROM users').get().count;
  const noteCount = db.prepare('SELECT COUNT(*) AS count FROM notes').get().count;
  const auditLogs = db.prepare(
    'SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 50'
  ).all();
  res.render('admin/dashboard', { userCount, noteCount, auditLogs });
}

function getUsers(req, res, db) {
  res.setHeader('Cache-Control', 'no-store');
  const users = userModel.getAllUsers(db);
  res.render('admin/users', { users });
}

function getReauth(req, res) {
  res.render('admin/reauth', { error: null });
}

async function postReauth(req, res, db) {
  const { password } = req.body;
  const admin = userModel.findById(db, req.session.userId);

  if (!admin) return res.redirect('/login');

  const valid = await passwordService.verifyPassword(password, admin.password_hash);
  if (!valid) {
    return res.render('admin/reauth', { error: 'Incorrect password.' });
  }

  req.session.reAuthVerified = true;
  auditService.log({ eventType: 'ADMIN_REAUTH', userId: req.session.userId, ipAddress: req.ip });
  const returnTo = req.session.returnTo || '/admin';
  delete req.session.returnTo;
  res.redirect(returnTo);
}

function postChangeRole(req, res, db) {
  res.setHeader('Cache-Control', 'no-store');
  const targetId = req.params.id;
  const { role } = req.body;

  if (!['user', 'admin'].includes(role)) {
    req.session.flash = { error: 'Invalid role.' };
    return res.redirect('/admin/users');
  }
  if (targetId === req.session.userId) {
    req.session.flash = { error: 'Cannot change your own role.' };
    return res.redirect('/admin/users');
  }

  userModel.updateRole(db, targetId, role);
  auditService.log({ eventType: 'ROLE_CHANGED', userId: req.session.userId, ipAddress: req.ip, resourceType: 'user', resourceId: targetId, details: { newRole: role } });

  delete req.session.reAuthVerified;
  req.session.flash = { success: 'Role updated.' };
  res.redirect('/admin/users');
}

function postReassignNote(req, res, db) {
  res.setHeader('Cache-Control', 'no-store');
  const noteId = req.params.id;
  const { newOwnerId } = req.body;

  const note = noteModel.findById(db, noteId);
  if (!note) {
    req.session.flash = { error: 'Note not found.' };
    return res.redirect('/admin');
  }

  const newOwner = userModel.findById(db, newOwnerId);
  if (!newOwner) {
    req.session.flash = { error: 'Target user not found.' };
    return res.redirect('/admin');
  }

  db.prepare('UPDATE notes SET user_id = ?, updated_at = ? WHERE id = ?').run(newOwnerId, Date.now(), noteId);
  auditService.log({ eventType: 'NOTE_REASSIGNED', userId: req.session.userId, ipAddress: req.ip, resourceType: 'note', resourceId: noteId, details: { newOwnerId } });

  delete req.session.reAuthVerified;
  req.session.flash = { success: 'Note reassigned.' };
  res.redirect('/admin');
}

module.exports = { getDashboard, getUsers, getReauth, postReauth, postChangeRole, postReassignNote };
