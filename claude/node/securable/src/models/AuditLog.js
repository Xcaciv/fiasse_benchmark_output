'use strict';

const { DataTypes } = require('sequelize');

/**
 * Append-only audit log table.
 * No update/delete operations should be performed on this table (Accountability).
 * Sensitive data (passwords, tokens) must never be stored here.
 */
module.exports = (sequelize) => {
  const AuditLog = sequelize.define(
    'AuditLog',
    {
      id: {
        type: DataTypes.UUID,
        defaultValue: DataTypes.UUIDV4,
        primaryKey: true,
      },
      userId: {
        type: DataTypes.UUID,
        allowNull: true,
        references: { model: 'users', key: 'id' },
      },
      action: {
        type: DataTypes.STRING(100),
        allowNull: false,
      },
      targetType: {
        type: DataTypes.STRING(50),
        allowNull: true,
      },
      targetId: {
        type: DataTypes.STRING(128),
        allowNull: true,
      },
      metadata: {
        type: DataTypes.TEXT,
        allowNull: true,
        get() {
          const raw = this.getDataValue('metadata');
          return raw ? JSON.parse(raw) : null;
        },
        set(value) {
          this.setDataValue('metadata', value ? JSON.stringify(value) : null);
        },
      },
      ipAddress: {
        type: DataTypes.STRING(45),
        allowNull: true,
      },
    },
    {
      tableName: 'audit_logs',
      updatedAt: false,
      indexes: [
        { fields: ['user_id'] },
        { fields: ['action'] },
        { fields: ['created_at'] },
      ],
    }
  );

  AuditLog.associate = (models) => {
    AuditLog.belongsTo(models.User, { foreignKey: 'userId', as: 'actor' });
  };

  return AuditLog;
};
