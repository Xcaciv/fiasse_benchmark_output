'use strict';

const express = require('express');
const { body, query } = require('express-validator');
const { Op } = require('sequelize');
const { User, Note, AuditLog } = require('../models');
const { requireAdmin } = require('../middleware/auth');
const { handleValidation } = require('../middleware/validate');
const audit = require('../services/auditService');

const router = express.Router();

// All admin routes require admin role — enforced at route level (defense in depth)
router.use(requireAdmin);

// ── Dashboard ─────────────────────────────────────────────────────────────

router.get('/', async (req, res, next) => {
  try {
    const [userCount, noteCount, recentLogs] = await Promise.all([
      User.count(),
      Note.count(),
      AuditLog.findAll({
        order: [['createdAt', 'DESC']],
        limit: 20,
        include: [{ model: User, as: 'actor', attributes: ['username'] }],
      }),
    ]);

    res.render('admin/dashboard', {
      title: 'Admin Dashboard',
      userCount,
      noteCount,
      recentLogs,
    });
  } catch (err) {
    next(err);
  }
});

// ── User List ──────────────────────────────────────────────────────────────

router.get('/users',
  query('q').optional().trim().isLength({ max: 100 }),
  handleValidation,
  async (req, res, next) => {
    try {
      const q = (req.query.q || '').trim();
      const where = q
        ? { [Op.or]: [{ username: { [Op.like]: `%${q}%` } }, { email: { [Op.like]: `%${q}%` } }] }
        : {};

      const users = await User.findAll({
        where,
        attributes: ['id', 'username', 'email', 'role', 'isLocked', 'createdAt'],
        order: [['createdAt', 'DESC']],
      });

      // Fetch note counts per user separately to keep query simple
      const noteCounts = await Note.findAll({
        attributes: ['userId', [require('sequelize').fn('COUNT', require('sequelize').col('id')), 'count']],
        group: ['userId'],
        raw: true,
      });

      const noteCountMap = Object.fromEntries(noteCounts.map((r) => [r.userId, r.count]));
      const usersWithCounts = users.map((u) => ({
        ...u.toJSON(),
        noteCount: noteCountMap[u.id] || 0,
      }));

      res.render('admin/users', { title: 'User Management', users: usersWithCounts, q });
    } catch (err) {
      next(err);
    }
  }
);

// ── Reassign Note ──────────────────────────────────────────────────────────

router.get('/reassign/:noteId', async (req, res, next) => {
  try {
    const note = await Note.findByPk(req.params.noteId, {
      include: [{ model: User, as: 'owner', attributes: ['username'] }],
    });
    if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', statusCode: 404 });

    const users = await User.findAll({ attributes: ['id', 'username'], order: [['username', 'ASC']] });
    return res.render('admin/reassign', {
      title: 'Reassign Note',
      note,
      users,
      csrfToken: req.csrfToken(),
    });
  } catch (err) {
    return next(err);
  }
});

router.post('/reassign/:noteId',
  body('newOwnerId').trim().notEmpty().withMessage('New owner required').isUUID().withMessage('Invalid user ID'),
  handleValidation,
  async (req, res, next) => {
    try {
      const note = await Note.findByPk(req.params.noteId);
      if (!note) return res.status(404).render('error', { title: 'Not Found', message: 'Note not found.', statusCode: 404 });

      const newOwner = await User.findByPk(req.body.newOwnerId);
      if (!newOwner) {
        req.flash('error', 'Target user not found.');
        return res.redirect(`/admin/reassign/${note.id}`);
      }

      const previousOwnerId = note.userId;
      await note.update({ userId: newOwner.id });

      await audit.record('admin.note_reassigned', req.user.id, {
        noteId: note.id,
        previousOwnerId,
        newOwnerId: newOwner.id,
      }, req.ip);

      req.flash('success', `Note reassigned to ${newOwner.username}.`);
      return res.redirect('/admin/users');
    } catch (err) {
      return next(err);
    }
  }
);

module.exports = router;
