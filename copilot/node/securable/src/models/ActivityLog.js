'use strict';

const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const ActivityLog = sequelize.define('ActivityLog', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  userId: {
    type: DataTypes.UUID,
    allowNull: true,
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
    type: DataTypes.STRING(255),
    allowNull: true,
  },
  details: {
    type: DataTypes.TEXT,
    allowNull: true,
  },
  ipAddress: {
    type: DataTypes.STRING(45),
    allowNull: true,
  },
}, {
  tableName: 'activity_logs',
  updatedAt: false,
});

module.exports = ActivityLog;
