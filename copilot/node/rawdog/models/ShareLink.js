const { DataTypes } = require('sequelize');
const { v4: uuidv4 } = require('uuid');
const sequelize = require('../config/database');

const ShareLink = sequelize.define('ShareLink', {
  id: {
    type: DataTypes.UUID,
    defaultValue: () => uuidv4(),
    primaryKey: true,
  },
  noteId: {
    type: DataTypes.UUID,
    allowNull: false,
  },
  token: {
    type: DataTypes.UUID,
    defaultValue: () => uuidv4(),
    unique: true,
  },
}, {
  timestamps: true,
  updatedAt: false,
});

module.exports = ShareLink;
