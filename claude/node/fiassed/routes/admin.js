'use strict';

const express = require('express');
const { body, validationResult } = require('express-validator');
const { authenticate } = require('../middleware/authenticate');
const { requireAdmin } = require('../middleware/authorize');
const { noCacheForPrivate } = require('../middleware/security');
const adminService = require('../services/adminService');
const { User, Note } = require('../models');
const auditService = require('../services/auditService');
const constants = require('../config/constants');

const router = express.Router();

// All admin routes require authentication AND admin role
router.use(authenticate, requireAdmin, noCacheForPrivate);

// ─── Dashboard ────────────────────────────────────────────────────────────────

router.get('/dashboard', async (req, res, next) => {
  try {
    const stats = await adminService.getDashboardStats();
    res.render('admin/dashboard', {
      title: 'Admin Dashboard',
      stats,
      currentUser: req.currentUser
    });
  } catch (err) {
    next(err);
  }
});

// ─── User Management ──────────────────────────────────────────────────────────

router.get('/users', async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const search = req.query.search || '';

    const { rows: users, count } = await adminService.getUsers(
      page,
      constants.PAGINATION.DEFAULT_PAGE_SIZE,
      search
    );

    res.render('admin/users', {
      title: 'User Management',
      users,
      count,
      page,
      pageSize: constants.PAGINATION.DEFAULT_PAGE_SIZE,
      search,
      csrfToken: req.csrfToken(),
      currentUser: req.currentUser
    });
  } catch (err) {
    next(err);
  }
});

// Toggle user active status
router.post('/users/:userId/toggle-active', [
  body('isActive').isBoolean()
], async (req, res, next) => {
  try {
    const isActive = req.body.isActive === 'true' || req.body.isActive === true;

    await adminService.setUserActiveStatus(
      req.params.userId,
      req.currentUser.id,
      isActive,
      req.correlationId,
      req.ip
    );

    req.flash('success', `User ${isActive ? 'activated' : 'deactivated'}.`);
    res.redirect('/admin/users');
  } catch (err) {
    next(err);
  }
});

// ─── Note Reassignment ────────────────────────────────────────────────────────

router.get('/reassign/:noteId', async (req, res, next) => {
  try {
    const note = await Note.findByPk(req.params.noteId);
    if (!note) {
      return res.status(404).render('error', {
        title: 'Not Found',
        message: 'Note not found.',
        correlationId: req.correlationId,
        currentUser: req.currentUser
      });
    }

    const { rows: users } = await adminService.getUsers(1, 100);

    res.render('admin/reassign', {
      title: 'Reassign Note',
      note,
      users,
      csrfToken: req.csrfToken(),
      errors: [],
      currentUser: req.currentUser
    });
  } catch (err) {
    next(err);
  }
});

router.post('/reassign/:noteId', [
  body('newOwnerId').isUUID()
], async (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    req.flash('error', 'Invalid user selection.');
    return res.redirect(`/admin/reassign/${req.params.noteId}`);
  }

  try {
    const success = await adminService.reassignNote(
      req.params.noteId,
      req.body.newOwnerId,
      req.currentUser.id,
      req.correlationId,
      req.ip
    );

    if (!success) {
      req.flash('error', 'Reassignment failed. Note or user not found.');
      return res.redirect(`/admin/reassign/${req.params.noteId}`);
    }

    req.flash('success', 'Note ownership transferred.');
    res.redirect('/admin/dashboard');
  } catch (err) {
    next(err);
  }
});

module.exports = router;
