'use strict';

const { Op } = require('sequelize');
const { User, Note, ActivityLog } = require('../models');
const { logActivity } = require('../services/auditService');
const logger = require('../utils/logger');

const PAGE_SIZE = 20;

async function getDashboard(req, res, next) {
  try {
    const [userCount, noteCount, recentLogs] = await Promise.all([
      User.count(),
      Note.count(),
      ActivityLog.findAll({
        order: [['createdAt', 'DESC']],
        limit: 20,
        include: [{ model: User, as: 'actor', attributes: ['username'] }],
      }),
    ]);
    res.render('admin/dashboard', { title: 'Admin Dashboard', userCount, noteCount, recentLogs, csrfToken: req.csrfToken() });
  } catch (err) {
    next(err);
  }
}

async function getUsers(req, res, next) {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const offset = (page - 1) * PAGE_SIZE;
    const { count, rows: users } = await User.findAndCountAll({
      order: [['createdAt', 'DESC']],
      limit: PAGE_SIZE,
      offset,
    });
    const noteCounts = await Note.findAll({
      where: { userId: users.map((u) => u.id) },
      attributes: ['userId', [require('sequelize').fn('COUNT', require('sequelize').col('id')), 'count']],
      group: ['userId'],
      raw: true,
    });
    const noteCountMap = Object.fromEntries(noteCounts.map((r) => [r.userId, parseInt(r.count, 10)]));
    res.render('admin/users', {
      title: 'Manage Users',
      users,
      noteCountMap,
      page,
      totalPages: Math.ceil(count / PAGE_SIZE),
      searchQuery: '',
      csrfToken: req.csrfToken(),
    });
  } catch (err) {
    next(err);
  }
}

async function searchUsers(req, res, next) {
  try {
    const q = (req.query.q || '').trim().slice(0, 100);
    const DIALECT = process.env.DATABASE_URL && process.env.DATABASE_URL.startsWith('postgres') ? 'postgres' : 'sqlite';
    const likeOp = DIALECT === 'postgres' ? Op.iLike : Op.like;
    const users = q ? await User.findAll({
      where: { [Op.or]: [{ username: { [likeOp]: `%${q}%` } }, { email: { [likeOp]: `%${q}%` } }] },
      limit: 50,
    }) : [];
    res.render('admin/users', { title: 'User Search', users, noteCountMap: {}, page: 1, totalPages: 1, searchQuery: q, csrfToken: req.csrfToken() });
  } catch (err) {
    next(err);
  }
}

async function postReassignNote(req, res, next) {
  try {
    const { id } = req.params;
    const { newOwnerId } = req.body;
    const note = await Note.findByPk(id);
    if (!note) return res.status(404).render('error', { statusCode: 404, message: 'Note not found.' });
    const newOwner = await User.findByPk(newOwnerId);
    if (!newOwner) return res.status(404).render('error', { statusCode: 404, message: 'User not found.' });
    const oldOwnerId = note.userId;
    await note.update({ userId: newOwnerId });
    await logActivity({
      userId: req.user.id,
      action: 'admin.note.reassign',
      targetType: 'Note',
      targetId: note.id,
      details: { from: oldOwnerId, to: newOwnerId },
      ipAddress: req.ip,
    });
    req.flash('success', 'Note ownership reassigned.');
    return res.redirect('/admin/users');
  } catch (err) {
    return next(err);
  }
}

module.exports = { getDashboard, getUsers, searchUsers, postReassignNote };
