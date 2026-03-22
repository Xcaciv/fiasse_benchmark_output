const express = require('express');
const router = express.Router();
const { User, Note } = require('../models');
const { ensureAuthenticated, ensureAdmin } = require('../middleware/auth');
const { Op } = require('sequelize');
const bcrypt = require('bcrypt');
const logger = require('../config/logger');

// GET /admin
router.get('/', ensureAuthenticated, ensureAdmin, async (req, res, next) => {
  try {
    const userCount = await User.count();
    const noteCount = await Note.count();
    const recentUsers = await User.findAll({
      order: [['createdAt', 'DESC']],
      limit: 5,
      attributes: ['id', 'username', 'email', 'role', 'createdAt'],
    });
    const recentNotes = await Note.findAll({
      order: [['createdAt', 'DESC']],
      limit: 5,
      include: [{ model: User, as: 'author', attributes: ['username'] }],
    });
    logger.info(`Admin dashboard accessed by: ${req.user.email}`);
    res.render('admin/dashboard', {
      title: 'Admin Dashboard',
      user: req.user,
      userCount,
      noteCount,
      recentUsers,
      recentNotes,
      messages: req.flash(),
    });
  } catch (err) {
    next(err);
  }
});

// GET /admin/users
router.get('/users', ensureAuthenticated, ensureAdmin, async (req, res, next) => {
  const q = (req.query.q || '').trim();
  try {
    const where = q
      ? { [Op.or]: [{ username: { [Op.like]: `%${q}%` } }, { email: { [Op.like]: `%${q}%` } }] }
      : {};
    const users = await User.findAll({
      where,
      order: [['createdAt', 'DESC']],
      attributes: ['id', 'username', 'email', 'role', 'createdAt'],
    });
    // Get note counts
    const noteCountsRaw = await Note.findAll({
      attributes: ['userId', [require('sequelize').fn('COUNT', require('sequelize').col('id')), 'count']],
      group: ['userId'],
    });
    const noteCounts = {};
    noteCountsRaw.forEach(n => { noteCounts[n.userId] = parseInt(n.get('count')); });

    res.render('admin/users', {
      title: 'User Management',
      user: req.user,
      users,
      noteCounts,
      q,
      messages: req.flash(),
    });
  } catch (err) {
    next(err);
  }
});

// POST /admin/users/:id/delete
router.post('/users/:id/delete', ensureAuthenticated, ensureAdmin, async (req, res, next) => {
  try {
    if (req.params.id === req.user.id) {
      req.flash('error', 'You cannot delete your own account.');
      return res.redirect('/admin/users');
    }
    await User.destroy({ where: { id: req.params.id } });
    logger.info(`Admin ${req.user.email} deleted user ${req.params.id}`);
    req.flash('success', 'User deleted.');
    res.redirect('/admin/users');
  } catch (err) {
    next(err);
  }
});

// POST /admin/users/:id/toggle-role
router.post('/users/:id/toggle-role', ensureAuthenticated, ensureAdmin, async (req, res, next) => {
  try {
    const target = await User.findByPk(req.params.id);
    if (!target) {
      req.flash('error', 'User not found.');
      return res.redirect('/admin/users');
    }
    const newRole = target.role === 'admin' ? 'user' : 'admin';
    await target.update({ role: newRole });
    logger.info(`Admin ${req.user.email} changed role of ${target.email} to ${newRole}`);
    req.flash('success', `User role changed to ${newRole}.`);
    res.redirect('/admin/users');
  } catch (err) {
    next(err);
  }
});

// POST /admin/notes/:id/reassign
router.post('/notes/:id/reassign', ensureAuthenticated, ensureAdmin, async (req, res, next) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) {
      req.flash('error', 'Note not found.');
      return res.redirect('/admin');
    }
    const newOwner = await User.findOne({ where: { username: req.body.newOwner } });
    if (!newOwner) {
      req.flash('error', 'Target user not found.');
      return res.redirect('/admin');
    }
    await note.update({ userId: newOwner.id });
    logger.info(`Admin ${req.user.email} reassigned note ${note.id} to ${newOwner.email}`);
    req.flash('success', `Note reassigned to ${newOwner.username}.`);
    res.redirect('/admin');
  } catch (err) {
    next(err);
  }
});

module.exports = router;
