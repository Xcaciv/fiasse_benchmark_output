'use strict';

const { DataTypes } = require('sequelize');

module.exports = (sequelize) => {
  const AuditLog = sequelize.define('AuditLog', {
    id: {
      type: DataTypes.UUID,
      defaultValue: DataTypes.UUIDV4,
      primaryKey: true,
    },
    // userId null for unauthenticated actions (e.g., failed login)
    userId: {
      type: DataTypes.UUID,
      allowNull: true,
    },
    action: {
      type: DataTypes.STRING(100),
      allowNull: false,
    },
    // metadata: structured JSON — Accountability, no sensitive data stored here
    metadata: {
      type: DataTypes.JSON,
      defaultValue: {},
    },
    ipAddress: {
      type: DataTypes.STRING(45), // supports IPv6
      allowNull: true,
    },
  }, {
    tableName: 'audit_logs',
    updatedAt: false, // audit records are immutable after creation
    indexes: [
      { fields: ['userId'] },
      { fields: ['action'] },
      { fields: ['createdAt'] },
    ],
  });

  AuditLog.associate = (models) => {
    AuditLog.belongsTo(models.User, { foreignKey: 'userId', as: 'actor' });
  };

  return AuditLog;
};
