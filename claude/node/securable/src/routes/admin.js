'use strict';

const express = require('express');
const { body, param, query } = require('express-validator');
const { Op } = require('sequelize');
const { User, Note, AuditLog } = require('../models');
const noteService = require('../services/noteService');
const { handleValidationErrors } = require('../middleware/validate');
const { requireAdmin } = require('../middleware/auth');

const router = express.Router();

router.use(requireAdmin);

// GET /admin — dashboard
router.get('/', async (req, res, next) => {
  try {
    const [userCount, noteCount, recentActivity] = await Promise.all([
      User.count(),
      Note.count(),
      AuditLog.findAll({
        limit: 20,
        order: [['created_at', 'DESC']],
        include: [{ association: 'actor', attributes: ['id', 'username'] }],
      }),
    ]);
    res.render('admin/dashboard', {
      title: 'Admin Dashboard',
      userCount,
      noteCount,
      recentActivity,
    });
  } catch (err) {
    next(err);
  }
});

// GET /admin/users
router.get(
  '/users',
  [query('search').optional().trim().isLength({ max: 100 })],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { search } = req.query;
      const where = search
        ? {
            [Op.or]: [
              { username: { [Op.like]: `%${search}%` } },
              { email: { [Op.like]: `%${search}%` } },
            ],
          }
        : {};

      const users = await User.findAll({
        where,
        attributes: ['id', 'username', 'email', 'role', 'isActive', 'created_at'],
        include: [{ model: Note, as: 'notes', attributes: ['id'] }],
        order: [['created_at', 'DESC']],
      });

      res.render('admin/users', { title: 'Manage Users', users, search: search || '' });
    } catch (err) {
      next(err);
    }
  }
);

// GET /admin/reassign/:noteId
router.get(
  '/reassign/:noteId',
  [param('noteId').isUUID().withMessage('Invalid note ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const note = await Note.findByPk(req.params.noteId, {
        include: [{ model: User, as: 'owner', attributes: ['id', 'username'] }],
      });
      if (!note) {
        req.flash('error', 'Note not found.');
        return res.redirect('/admin/users');
      }
      const users = await User.findAll({
        attributes: ['id', 'username'],
        order: [['username', 'ASC']],
      });
      res.render('admin/reassign', { title: 'Reassign Note', note, users });
    } catch (err) {
      next(err);
    }
  }
);

// POST /admin/reassign/:noteId
router.post(
  '/reassign/:noteId',
  [
    param('noteId').isUUID().withMessage('Invalid note ID.'),
    body('newUserId').isUUID().withMessage('Please select a valid user.'),
  ],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      const { newUserId } = req.body;
      // Verify target user exists
      const targetUser = await User.findByPk(newUserId, { attributes: ['id'] });
      if (!targetUser) {
        req.flash('error', 'Selected user does not exist.');
        return res.redirect(`/admin/reassign/${req.params.noteId}`);
      }
      await noteService.reassignNote(req.params.noteId, newUserId, req.user.id);
      req.flash('success', 'Note ownership reassigned.');
      res.redirect('/admin/users');
    } catch (err) {
      if (err.status === 404) {
        req.flash('error', err.message);
        return res.redirect('/admin/users');
      }
      next(err);
    }
  }
);

// POST /admin/users/:id/toggle-active
router.post(
  '/users/:id/toggle-active',
  [param('id').isUUID().withMessage('Invalid user ID.')],
  handleValidationErrors,
  async (req, res, next) => {
    try {
      if (req.params.id === req.user.id) {
        req.flash('error', 'You cannot deactivate your own account.');
        return res.redirect('/admin/users');
      }
      const user = await User.findByPk(req.params.id, { attributes: ['id', 'isActive'] });
      if (!user) {
        req.flash('error', 'User not found.');
        return res.redirect('/admin/users');
      }
      await user.update({ isActive: !user.isActive });
      req.flash('success', `User account ${user.isActive ? 'activated' : 'deactivated'}.`);
      res.redirect('/admin/users');
    } catch (err) {
      next(err);
    }
  }
);

module.exports = router;
