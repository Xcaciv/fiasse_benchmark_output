'use strict';

const { Op } = require('sequelize');
const { User, Note, AuditLog } = require('../models/index');
const { logAction, AUDIT_ACTIONS } = require('./auditService');

const PAGE_SIZE = 20;

/** Aggregate dashboard statistics and recent audit activity. */
const getStats = async () => {
  const [totalUsers, totalNotes, recentActivity] = await Promise.all([
    User.count(),
    Note.count(),
    AuditLog.findAll({
      order: [['createdAt', 'DESC']],
      limit: 20,
      include: [{ model: User, attributes: ['id', 'username'], as: 'actor', required: false }],
    }),
  ]);

  return { totalUsers, totalNotes, recentActivity };
};

/** List users with optional search, paginated. */
const listUsers = async ({ search = '', page = 1 } = {}) => {
  const offset = (page - 1) * PAGE_SIZE;

  const where = search
    ? {
        [Op.or]: [
          { username: { [Op.like]: `%${search}%` } },
          { email: { [Op.like]: `%${search}%` } },
        ],
      }
    : {};

  const { count, rows } = await User.findAndCountAll({
    where,
    attributes: ['id', 'username', 'email', 'role', 'createdAt'],
    order: [['createdAt', 'DESC']],
    limit: PAGE_SIZE,
    offset,
    distinct: true,
  });

  // Fetch note counts in a single query per user to avoid N+1
  const usersWithCounts = await Promise.all(
    rows.map(async (user) => {
      const noteCount = await Note.count({ where: { userId: user.id } });
      return { ...user.toJSON(), noteCount };
    })
  );

  return { users: usersWithCounts, total: count, page, pageSize: PAGE_SIZE };
};

/** Reassign note ownership. Admin-only action. */
const reassignNote = async ({ noteId, newOwnerId, adminId, ipAddress }) => {
  const note = await Note.findByPk(noteId);
  if (!note) {
    const err = new Error('Note not found'); err.status = 404; throw err;
  }

  const newOwner = await User.findByPk(newOwnerId);
  if (!newOwner) {
    const err = new Error('Target user not found'); err.status = 404; throw err;
  }

  const previousOwnerId = note.userId;
  await note.update({ userId: newOwnerId });

  logAction({
    actorId: adminId,
    action: AUDIT_ACTIONS.ADMIN_NOTE_REASSIGN,
    resourceType: 'Note',
    resourceId: noteId,
    metadata: { previousOwnerId, newOwnerId },
    ipAddress,
  });

  return note;
};

module.exports = { getStats, listUsers, reassignNote };
