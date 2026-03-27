'use strict';

const express = require('express');
const { requireAuth, requireAdmin } = require('../middleware/authenticate');
const adminService = require('../services/adminService');
const { User } = require('../models/index');
const { logAction, AUDIT_ACTIONS } = require('../services/auditService');

const router = express.Router();

// All admin routes require authentication and admin role
router.use(requireAuth, requireAdmin);

// GET /admin — dashboard
router.get('/', async (req, res, next) => {
  try {
    logAction({
      actorId: req.session.userId,
      action: AUDIT_ACTIONS.ADMIN_USER_VIEW,
      resourceType: 'AdminDashboard',
      resourceId: null,
      metadata: {},
      ipAddress: req.ip,
    });

    const stats = await adminService.getStats();

    res.render('admin/dashboard', {
      title: 'Admin Dashboard',
      ...stats,
      user: res.locals.user,
    });
  } catch (err) {
    next(err);
  }
});

// GET /admin/users — user management list
router.get('/users', async (req, res, next) => {
  try {
    const search = typeof req.query.search === 'string' ? req.query.search.trim() : '';
    const page = parseInt(req.query.page, 10) || 1;

    const result = await adminService.listUsers({ search, page });

    // Load all users for the reassignment dropdown
    const allUsers = await User.findAll({
      attributes: ['id', 'username'],
      order: [['username', 'ASC']],
    });

    res.render('admin/users', {
      title: 'Manage Users',
      ...result,
      search,
      allUsers,
      user: res.locals.user,
    });
  } catch (err) {
    next(err);
  }
});

// PUT /admin/notes/:noteId/owner — reassign note ownership
router.put('/notes/:noteId/owner', async (req, res, next) => {
  try {
    // Request Surface Minimization: extract only expected field
    const newOwnerId = typeof req.body.newOwnerId === 'string' ? req.body.newOwnerId.trim() : null;

    if (!newOwnerId) {
      req.session.flash = { type: 'error', message: 'A new owner must be selected.' };
      return res.redirect('/admin/users');
    }

    await adminService.reassignNote({
      noteId: req.params.noteId,
      newOwnerId,
      adminId: req.session.userId,
      ipAddress: req.ip,
    });

    req.session.flash = { type: 'success', message: 'Note ownership reassigned.' };
    res.redirect('/admin/users');
  } catch (err) {
    if (err.status === 404) {
      req.session.flash = { type: 'error', message: err.message };
      return res.redirect('/admin/users');
    }
    next(err);
  }
});

module.exports = router;
