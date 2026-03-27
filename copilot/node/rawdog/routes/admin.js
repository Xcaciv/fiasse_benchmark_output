const express = require('express');
const { Op } = require('sequelize');
const { User, Note, ActivityLog } = require('../models');
const { isAuthenticated, isAdmin } = require('../middleware/auth');

const router = express.Router();

// GET /admin
router.get('/', isAuthenticated, isAdmin, async (req, res) => {
  try {
    const totalUsers = await User.count();
    const totalNotes = await Note.count();
    const recentActivity = await ActivityLog.findAll({
      include: [{ model: User, attributes: ['username'], required: false }],
      order: [['createdAt', 'DESC']],
      limit: 20,
    });
    res.render('admin/dashboard', { title: 'Admin Dashboard', totalUsers, totalNotes, recentActivity });
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to load dashboard.');
    res.redirect('/notes');
  }
});

// GET /admin/users
router.get('/users', isAuthenticated, isAdmin, async (req, res) => {
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
      include: [{ model: Note, attributes: ['id'] }],
      order: [['createdAt', 'DESC']],
    });
    res.render('admin/users', { title: 'Manage Users', users, search: search || '' });
  } catch (err) {
    console.error(err);
    req.flash('error', 'Failed to load users.');
    res.redirect('/admin');
  }
});

// POST /admin/notes/:id/reassign
router.post('/notes/:id/reassign', isAuthenticated, isAdmin, async (req, res) => {
  try {
    const note = await Note.findByPk(req.params.id);
    if (!note) {
      req.flash('error', 'Note not found.');
      return res.redirect('/admin');
    }
    const { newUserId } = req.body;
    const newUser = await User.findByPk(newUserId);
    if (!newUser) {
      req.flash('error', 'Target user not found.');
      return res.redirect('/admin');
    }
    const oldUserId = note.userId;
    await note.update({ userId: newUserId });
    await ActivityLog.create({
      userId: req.session.userId,
      action: 'admin_reassign',
      details: `Admin reassigned note "${note.title}" from ${oldUserId} to ${newUser.username}`,
    });
    req.flash('success', `Note reassigned to ${newUser.username}.`);
    res.redirect('/admin/users');
  } catch (err) {
    console.error(err);
    req.flash('error', 'Reassignment failed.');
    res.redirect('/admin');
  }
});

module.exports = router;
