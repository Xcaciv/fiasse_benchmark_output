const express = require('express');
const router = express.Router();
const User = require('../models/User');
const Note = require('../models/Note');
const ActivityLog = require('../models/ActivityLog');
const { requireAuth, requireAdmin } = require('../middleware/auth');
const logger = require('../utils/logger');

router.get('/', requireAdmin, async (req, res) => {
  try {
    const userCount = await User.count();
    const noteCount = await Note.count();
    const recentActivity = await ActivityLog.getRecent(20);
    
    res.render('admin/index', {
      user: req.user,
      userCount,
      noteCount,
      recentActivity
    });
  } catch (error) {
    logger.error('Error loading admin dashboard', { error: error.message });
    req.flash('error', 'Failed to load dashboard');
    res.redirect('/');
  }
});

router.get('/users', requireAdmin, async (req, res) => {
  try {
    const search = req.query.search || null;
    const users = await User.getAll(search);
    
    res.render('admin/users', { user: req.user, users, search });
  } catch (error) {
    logger.error('Error fetching users', { error: error.message });
    req.flash('error', 'Failed to load users');
    res.redirect('/admin');
  }
});

router.get('/users/:id', requireAdmin, async (req, res) => {
  try {
    const targetUser = await User.findById(req.params.id);
    
    if (!targetUser) {
      req.flash('error', 'User not found');
      return res.redirect('/admin/users');
    }
    
    const notes = await Note.getByUserId(targetUser.id);
    const activity = await ActivityLog.findByUserId(targetUser.id, 20);
    
    res.render('admin/user-detail', {
      user: req.user,
      targetUser,
      notes,
      activity
    });
  } catch (error) {
    logger.error('Error fetching user detail', { error: error.message });
    req.flash('error', 'Failed to load user');
    res.redirect('/admin/users');
  }
});

router.get('/notes', requireAdmin, async (req, res) => {
  try {
    const notes = await Note.getRecent(50);
    res.render('admin/notes', { user: req.user, notes });
  } catch (error) {
    logger.error('Error fetching notes', { error: error.message });
    req.flash('error', 'Failed to load notes');
    res.redirect('/admin');
  }
});

router.post('/notes/:id/reassign', requireAdmin, async (req, res) => {
  try {
    const { new_owner_id } = req.body;
    const note = await Note.findByIdWithOwner(req.params.id);
    
    if (!note) {
      req.flash('error', 'Note not found');
      return res.redirect('/admin/notes');
    }
    
    const newOwner = await User.findById(new_owner_id);
    if (!newOwner) {
      req.flash('error', 'New owner not found');
      return res.redirect('/admin/notes');
    }
    
    await Note.reassignOwner(req.params.id, new_owner_id);
    
    await ActivityLog.log(
      req.user.id,
      'NOTE_REASSIGNED',
      'note',
      req.params.id,
      `From user ${note.user_id} to user ${new_owner_id}`,
      req.ip
    );
    
    logger.info('Note reassigned', {
      noteId: req.params.id,
      fromUserId: note.user_id,
      toUserId: new_owner_id,
      adminId: req.user.id
    });
    
    req.flash('success', `Note reassigned to ${newOwner.username}`);
    res.redirect(`/admin/users/${new_owner_id}`);
  } catch (error) {
    logger.error('Error reassigning note', { error: error.message });
    req.flash('error', 'Failed to reassign note');
    res.redirect('/admin/notes');
  }
});

router.get('/activity', requireAdmin, async (req, res) => {
  try {
    const activity = await ActivityLog.getRecent(100);
    res.render('admin/activity', { user: req.user, activity });
  } catch (error) {
    logger.error('Error fetching activity', { error: error.message });
    req.flash('error', 'Failed to load activity log');
    res.redirect('/admin');
  }
});

module.exports = router;
