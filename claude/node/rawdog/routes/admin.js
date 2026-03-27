const express = require('express');
const router = express.Router();
const { Op } = require('sequelize');
const { body, validationResult } = require('express-validator');
const db = require('../models');
const { isAdmin } = require('../middleware/auth');

// GET /admin - Dashboard
router.get('/', isAdmin, async (req, res, next) => {
  try {
    const userCount = await db.User.count();
    const noteCount = await db.Note.count();
    const publicNoteCount = await db.Note.count({ where: { isPublic: true } });
    const attachmentCount = await db.Attachment.count();

    const recentActivity = await db.AuditLog.findAll({
      order: [['createdAt', 'DESC']],
      limit: 20,
      include: [{ model: db.User, attributes: ['username'] }]
    });

    res.render('admin/dashboard', {
      title: 'Admin Dashboard',
      stats: { userCount, noteCount, publicNoteCount, attachmentCount },
      recentActivity
    });
  } catch (err) {
    next(err);
  }
});

// GET /admin/users - User list
router.get('/users', isAdmin, async (req, res, next) => {
  try {
    const { q } = req.query;
    let whereClause = {};
    if (q && q.trim()) {
      whereClause = {
        [Op.or]: [
          { username: { [Op.like]: `%${q.trim()}%` } },
          { email: { [Op.like]: `%${q.trim()}%` } }
        ]
      };
    }

    const users = await db.User.findAll({
      where: whereClause,
      include: [{ model: db.Note, attributes: ['id'] }],
      order: [['createdAt', 'DESC']]
    });

    const usersWithCounts = users.map(u => ({
      ...u.toJSON(),
      noteCount: (u.Notes || []).length
    }));

    res.render('admin/users', { title: 'Manage Users', users: usersWithCounts, q: q || '' });
  } catch (err) {
    next(err);
  }
});

// GET /admin/reassign-note/:noteId
router.get('/reassign-note/:noteId', isAdmin, async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.noteId, {
      include: [{ model: db.User, as: 'author', attributes: ['username', 'id'] }]
    });

    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', statusCode: 404, message: 'Note not found.' });
    }

    const users = await db.User.findAll({ attributes: ['id', 'username', 'isAdmin'], order: [['username', 'ASC']] });

    res.render('admin/reassign-note', { title: 'Reassign Note', note: note.toJSON(), users, errors: [] });
  } catch (err) {
    next(err);
  }
});

// POST /admin/reassign-note/:noteId
router.post('/reassign-note/:noteId', isAdmin, [
  body('newOwnerId').notEmpty().withMessage('Please select a new owner.')
], async (req, res, next) => {
  try {
    const note = await db.Note.findByPk(req.params.noteId, {
      include: [{ model: db.User, as: 'author', attributes: ['username', 'id'] }]
    });

    if (!note) {
      return res.status(404).render('error', { title: 'Not Found', statusCode: 404, message: 'Note not found.' });
    }

    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      const users = await db.User.findAll({ attributes: ['id', 'username', 'isAdmin'], order: [['username', 'ASC']] });
      return res.render('admin/reassign-note', {
        title: 'Reassign Note',
        note: note.toJSON(),
        users,
        errors: errors.array()
      });
    }

    const { newOwnerId } = req.body;
    const newOwner = await db.User.findByPk(newOwnerId);
    if (!newOwner) {
      req.flash('error', 'Selected user not found.');
      return res.redirect(`/admin/reassign-note/${req.params.noteId}`);
    }

    const oldOwner = note.author ? note.author.username : 'unknown';
    await note.update({ userId: newOwnerId });

    await db.AuditLog.create({
      userId: req.user.id,
      action: 'note_reassign',
      details: `Note "${note.title}" (id=${note.id}) reassigned from ${oldOwner} to ${newOwner.username} by admin ${req.user.username}`,
      ipAddress: req.ip
    });

    req.flash('success_msg', `Note reassigned to ${newOwner.username}.`);
    res.redirect('/admin/users');
  } catch (err) {
    next(err);
  }
});

// POST /admin/toggle-admin/:userId
router.post('/toggle-admin/:userId', isAdmin, async (req, res, next) => {
  try {
    const user = await db.User.findByPk(req.params.userId);
    if (!user) {
      req.flash('error', 'User not found.');
      return res.redirect('/admin/users');
    }

    if (user.id === req.user.id) {
      req.flash('error', 'You cannot change your own admin status.');
      return res.redirect('/admin/users');
    }

    await user.update({ isAdmin: !user.isAdmin });

    await db.AuditLog.create({
      userId: req.user.id,
      action: 'admin_toggle',
      details: `Admin status toggled for user ${user.username} (now ${!user.isAdmin ? 'admin' : 'user'}) by ${req.user.username}`,
      ipAddress: req.ip
    });

    req.flash('success_msg', `Admin status updated for ${user.username}.`);
    res.redirect('/admin/users');
  } catch (err) {
    next(err);
  }
});

module.exports = router;
