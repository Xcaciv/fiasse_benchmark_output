const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');
const sequelize = require('../config/database');

const ActivityLog = sequelize.define('ActivityLog', {
  id: {
    type: DataTypes.UUID,
    defaultValue: () => uuidv4(),
    primaryKey: true,
  },
  userId: {
    type: DataTypes.UUID,
    allowNull: true,
  },
  action: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  details: {
    type: DataTypes.TEXT,
    allowNull: true,
  },
}, {
  timestamps: true,
  updatedAt: false,
});

module.exports = ActivityLog;
