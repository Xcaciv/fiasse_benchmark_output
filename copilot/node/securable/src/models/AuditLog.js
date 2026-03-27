'use strict';

const { DataTypes, Model } = require('sequelize');
const { sequelize } = require('../config/database');

/**
 * Append-only audit log. No updatedAt field — records are immutable once written.
 * Accountability: captures who did what, to which resource, from where, and when.
 */
class AuditLog extends Model {}

AuditLog.init({
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  actorId: {
    type: DataTypes.UUID,
    allowNull: true, // null for unauthenticated actions (e.g., login failure)
    references: { model: 'users', key: 'id' },
  },
  action: {
    type: DataTypes.STRING(100),
    allowNull: false,
  },
  resourceType: {
    type: DataTypes.STRING(100),
    allowNull: false,
  },
  resourceId: {
    type: DataTypes.STRING(255),
    allowNull: true,
  },
  // metadata is pre-sanitized before storage — never contains secrets
  metadata: {
    type: DataTypes.JSON,
    allowNull: true,
  },
  ipAddress: {
    type: DataTypes.STRING(45),
    allowNull: true,
  },
}, {
  sequelize,
  modelName: 'AuditLog',
  tableName: 'audit_logs',
  timestamps: true,
  updatedAt: false, // Append-only: no updates permitted
});

module.exports = { AuditLog };
