'use strict';

const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');

// Accountability: AuditLog is append-only - no update or delete methods
module.exports = (sequelize) => {
  const AuditLog = sequelize.define('AuditLog', {
    id: {
      type: DataTypes.UUID,
      defaultValue: () => uuidv4(),
      primaryKey: true,
      allowNull: false
    },
    event: {
      type: DataTypes.STRING(100),
      allowNull: false
    },
    actorId: {
      type: DataTypes.UUID,
      allowNull: true
    },
    targetId: {
      type: DataTypes.UUID,
      allowNull: true
    },
    targetType: {
      type: DataTypes.STRING(50),
      allowNull: true
    },
    outcome: {
      type: DataTypes.STRING(20),
      allowNull: false,
      defaultValue: 'success',
      validate: {
        isIn: [['success', 'failure', 'denied']]
      }
    },
    // Metadata stored as JSON text - no PII in this field beyond actor/target refs
    metadata: {
      type: DataTypes.TEXT,
      allowNull: true,
      get() {
        const raw = this.getDataValue('metadata');
        if (!raw) return null;
        try {
          return JSON.parse(raw);
        } catch {
          return null;
        }
      },
      set(value) {
        this.setDataValue('metadata', value ? JSON.stringify(value) : null);
      }
    },
    ip: {
      type: DataTypes.STRING(45),
      allowNull: true
    },
    correlationId: {
      type: DataTypes.UUID,
      allowNull: true
    }
  }, {
    tableName: 'audit_logs',
    timestamps: true,
    updatedAt: false
  });

  // No associations to User to avoid cascade deletes wiping audit trail
  AuditLog.associate = () => {};

  return AuditLog;
};
