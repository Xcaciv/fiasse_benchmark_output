'use strict';

const { Op } = require('sequelize');
const { User, Note, AuditLog } = require('../models');
const constants = require('../config/constants');
const noteService = require('./noteService');
const auditService = require('./auditService');

/**
 * Get paginated user list with optional search.
 * Confidentiality: passwordHash and reset fields excluded from all results.
 * @param {number} page
 * @param {number} limit
 * @param {string} [search]
 * @returns {Promise<{ rows: User[], count: number }>}
 */
async function getUsers(page = 1, limit = 20, search = '') {
  const offset = (page - 1) * limit;
  const safeLimit = Math.min(limit, constants.PAGINATION.MAX_PAGE_SIZE);

  const where = search
    ? {
        [Op.or]: [
          { username: { [Op.like]: `%${search}%` } },
          { email: { [Op.like]: `%${search}%` } }
        ]
      }
    : {};

  return User.findAndCountAll({
    where,
    attributes: ['id', 'username', 'email', 'role', 'isActive', 'createdAt'],
    order: [['createdAt', 'DESC']],
    limit: safeLimit,
    offset
  });
}

/**
 * Get dashboard statistics.
 * @returns {Promise<{ userCount: number, noteCount: number, recentAudit: AuditLog[] }>}
 */
async function getDashboardStats() {
  const [userCount, noteCount, recentAudit] = await Promise.all([
    User.count(),
    Note.count(),
    AuditLog.findAll({
      order: [['createdAt', 'DESC']],
      limit: 20,
      attributes: ['id', 'event', 'actorId', 'targetId', 'outcome', 'ip', 'createdAt']
    })
  ]);

  return { userCount, noteCount, recentAudit };
}

/**
 * Reassign note ownership - delegates to noteService.transferOwnership.
 * @param {string} noteId
 * @param {string} newOwnerId
 * @param {string} adminId
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<boolean>}
 */
async function reassignNote(noteId, newOwnerId, adminId, correlationId, ip) {
  return noteService.transferOwnership(noteId, newOwnerId, adminId, correlationId, ip);
}

/**
 * Toggle user active status (enable/disable account).
 * Admin cannot deactivate themselves.
 * @param {string} targetUserId
 * @param {string} adminId
 * @param {boolean} isActive
 * @param {string} [correlationId]
 * @param {string} [ip]
 * @returns {Promise<boolean>}
 */
async function setUserActiveStatus(targetUserId, adminId, isActive, correlationId, ip) {
  if (targetUserId === adminId) return false;

  const user = await User.findByPk(targetUserId);
  if (!user) return false;

  await user.update({ isActive });

  await auditService.log('admin.user_status_changed', {
    actorId: adminId,
    targetId: targetUserId,
    targetType: 'user',
    outcome: 'success',
    metadata: { isActive },
    ip,
    correlationId
  });

  return true;
}

module.exports = {
  getUsers,
  getDashboardStats,
  reassignNote,
  setUserActiveStatus
};
